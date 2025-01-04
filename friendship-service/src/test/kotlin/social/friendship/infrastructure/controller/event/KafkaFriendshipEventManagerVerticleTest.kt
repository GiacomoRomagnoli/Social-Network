package social.friendship.infrastructure.controller.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vertx.core.Vertx
import io.vertx.kafka.client.producer.KafkaProducerRecord
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import social.common.events.FriendshipRemoved
import social.common.events.FriendshipRequestRejected
import social.common.events.UserBlocked
import social.common.events.UserCreated
import social.friendship.domain.Friendship
import social.friendship.domain.FriendshipRequest
import social.friendship.infrastructure.DockerSQLTest
import social.friendship.social.friendship.domain.User
import social.friendship.social.friendship.infrastructure.persistence.sql.DatabaseCredentials
import social.friendship.social.friendship.infrastructure.persistence.sql.FriendshipRequestSQLRepository
import social.friendship.social.friendship.infrastructure.persistence.sql.FriendshipSQLRepository
import social.friendship.social.friendship.infrastructure.persistence.sql.UserSQLRepository
import java.io.File
import java.util.concurrent.CountDownLatch

object KafkaFriendshipEventManagerVerticleTest : DockerSQLTest() {
    private val logger = LogManager.getLogger(this::class.java)
    private val kafka = KafkaFriendshipEventManagerVerticle(DatabaseCredentials(host, "3307", database, user, password))
    private val user1 = User.of("user1")
    private val user2 = User.of("user2")
    private val userRepository = UserSQLRepository()
    private val friendship = Friendship.of(user1, user2)
    private val friendshipRepository = FriendshipSQLRepository()
    private val friendshipRequest = FriendshipRequest.of(user1, user2)
    private val friendshipRequestRepository = FriendshipRequestSQLRepository()
    lateinit var dockerComposeFile: File
    private val vertx = Vertx.vertx()
    private val mapper: ObjectMapper = jacksonObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
    }

    @JvmStatic
    @BeforeAll
    fun setUpAll() {
        dockerComposeFile = generateDockerComposeFile("social/friendship/infrastructure/controller/event/")
    }

    @BeforeEach
    fun setUp() {
        executeDockerComposeCmd(dockerComposeFile, "up", "--wait")
        startKafkaVerticle(vertx)
        connectToDatabase()
    }

    private fun startKafkaVerticle(vertx: Vertx) {
        val latch = CountDownLatch(1)
        vertx.deployVerticle(kafka).onComplete {
            latch.countDown()
            if (it.succeeded()) {
                logger.info("Kafka event manager verticle started")
            } else {
                logger.error("Failed to start kafka event manager verticle:", it.cause())
            }
        }
        latch.await()
    }

    private fun connectToDatabase() {
        listOf(userRepository, friendshipRepository, friendshipRequestRepository).forEach {
            it.connect(host, "3307", database, user, password)
        }
    }

    @AfterEach
    fun tearDown() {
        executeDockerComposeCmd(dockerComposeFile, "down", "-v")
    }

    @Test
    fun updatesDatabaseUponReceivingUserCreatedEvent() {
        val before = userRepository.findById(user1.id)
        val producer = KafkaFriendshipProducer.createProducer(vertx)
        val record = KafkaProducerRecord.create<String, String>(
            UserCreated.TOPIC,
            mapper.writeValueAsString(user1)
        )
        producer.write(record)
        logger.trace("Sent event: {}", record.value())

        // waits for the event to be processed
        val latch = CountDownLatch(1)
        lateinit var after: User
        vertx.eventBus().consumer<String>(UserCreated.TOPIC) {
            userRepository.findById(user1.id)?.let {
                after = it
                latch.countDown()
            }
        }
        latch.await()

        assertAll(
            { assertEquals(null, before) },
            { assertEquals(user1, after) }
        )
    }

    @Test
    fun updatesDatabaseUponReceivingUserBlockedEvent() {
        userRepository.save(user1)
        userRepository.save(user2)
        friendshipRequestRepository.save(friendshipRequest)
        friendshipRepository.save(friendship)
        val friendshipBefore = friendshipRepository.findById(friendship.id)
        val friendshipRequestBefore = friendshipRequestRepository.findById(friendshipRequest.id)

        val blockedUser = Pair(user1, user2)
        val producer = KafkaFriendshipProducer.createProducer(vertx)
        val record = KafkaProducerRecord.create<String, String>(
            UserBlocked.TOPIC,
            mapper.writeValueAsString(blockedUser)
        )

        producer.write(record)
        logger.trace("Sent event: {}", record.value())

        // waits for the event to be processed
        val latch = CountDownLatch(2)
        var friendshipRequestAfter: FriendshipRequest? = null
        var friendshipAfter: Friendship? = null
        vertx.eventBus().consumer<String>(FriendshipRemoved.TOPIC) {
            friendshipAfter = friendshipRepository.findById(friendship.id)
            latch.countDown()
        }
        vertx.eventBus().consumer<String>(FriendshipRequestRejected.TOPIC) {
            friendshipRequestAfter = friendshipRequestRepository.findById(friendshipRequest.id)
            latch.countDown()
        }
        latch.await()

        assertAll(
            { assertEquals(friendshipBefore, friendship) },
            { assertEquals(friendshipRequestBefore, friendshipRequest) },
            { assertEquals(null, friendshipAfter) },
            { assertEquals(null, friendshipRequestAfter) }
        )
    }
}
