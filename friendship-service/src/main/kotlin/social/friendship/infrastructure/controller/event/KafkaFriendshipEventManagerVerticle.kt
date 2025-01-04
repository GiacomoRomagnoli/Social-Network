package social.friendship.infrastructure.controller.event

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vertx.core.AbstractVerticle
import io.vertx.kafka.client.consumer.KafkaConsumer
import io.vertx.kafka.client.producer.KafkaProducer
import io.vertx.kafka.client.producer.KafkaProducerRecord
import org.apache.logging.log4j.LogManager
import social.common.events.FriendshipRemoved
import social.common.events.FriendshipRequestRejected
import social.common.events.UserBlocked
import social.common.events.UserCreated
import social.friendship.domain.Friendship
import social.friendship.domain.Friendship.FriendshipID
import social.friendship.domain.FriendshipRequest
import social.friendship.domain.FriendshipRequest.FriendshipRequestID
import social.friendship.social.friendship.domain.User
import social.friendship.social.friendship.domain.User.UserID
import social.friendship.social.friendship.domain.application.FriendshipService
import social.friendship.social.friendship.domain.application.FriendshipServiceImpl
import social.friendship.social.friendship.infrastructure.persistence.sql.DatabaseCredentials
import social.friendship.social.friendship.infrastructure.persistence.sql.FriendshipRequestSQLRepository
import social.friendship.social.friendship.infrastructure.persistence.sql.FriendshipSQLRepository
import social.friendship.social.friendship.infrastructure.persistence.sql.UserSQLRepository
import java.nio.file.Files
import java.nio.file.Paths

class KafkaFriendshipEventManagerVerticle(val credentials: DatabaseCredentials? = null) : AbstractVerticle() {
    private val logger = LogManager.getLogger(this::class)
    private val userSQLRepository = UserSQLRepository()
    private val friendshipRepository = FriendshipSQLRepository()
    private val friendshipRequestRepository = FriendshipRequestSQLRepository()
    private val userService: FriendshipService<UserID, User> = FriendshipServiceImpl(userSQLRepository)
    private val friendshipService: FriendshipService<FriendshipID, Friendship> = FriendshipServiceImpl(friendshipRepository)
    private val friendshipRequestService: FriendshipService<FriendshipRequestID, FriendshipRequest> = FriendshipServiceImpl(friendshipRequestRepository)
    private val events: MutableSet<String> = mutableSetOf(
        UserCreated.TOPIC,
        UserBlocked.TOPIC
    )
    private val mapper: ObjectMapper = jacksonObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
    }

    override fun start() {
        connectToDatabase()
        val consumer = generateConsumer()
        val producer = generateProducer()
        subscribeToEvents(consumer)
        handleEvents(consumer, producer)
    }

    private fun connectToDatabase() {
        if (credentials != null) {
            connectToDatabaseWith(credentials)
        } else {
            connectToDefaultDatabase()
        }
    }

    private fun generateConsumer(): KafkaConsumer<String, String> {
        return KafkaFriendshipConsumer.createConsumer(vertx)
    }

    private fun generateProducer(): KafkaProducer<String, String> {
        return KafkaFriendshipProducer.createProducer(vertx)
    }

    private fun subscribeToEvents(consumer: KafkaConsumer<String, String>) {
        consumer.subscribe(events) { result ->
            if (result.succeeded()) {
                logger.debug("Subscribed to events: {}", events)
            } else {
                logger.error("Failed to subscribe to events {}", events)
            }
        }
    }

    private fun handleEvents(consumer: KafkaConsumer<String, String>, producer: KafkaProducer<String, String>) {
        consumer.handler { record ->
            logger.debug("Received event: {}", record.value())
            when (record.topic()) {
                UserCreated.TOPIC -> {
                    mapper.readValue(record.value(), User::class.java).let {
                        userService.add(it)
                        vertx.eventBus().publish(UserCreated.TOPIC, record.value())
                    }
                }

                UserBlocked.TOPIC -> {
                    val usersPairTypeRef = object : TypeReference<Pair<User, User>>() {}
                    mapper.readValue(record.value(), usersPairTypeRef).let { (userBlocking, userToBlock) ->
                        val friendshipToRemove = removeFriendship(userBlocking, userToBlock)
                        produceFriendshipRemovedEvent(producer, friendshipToRemove)
                        vertx.eventBus().publish(FriendshipRemoved.TOPIC, record.value())
                        logger.trace("Removed friendship between {} and {}", userBlocking, userToBlock)

                        val friendshipRequestToRemove = removeFriendshipRequest(userBlocking, userToBlock)
                        produceFriendshipRejectedEvent(producer, friendshipRequestToRemove)
                        vertx.eventBus().publish(FriendshipRequestRejected.TOPIC, record.value())
                        logger.trace("Removed friendship request between {} and {}", userBlocking, userToBlock)
                    }
                }
            }
        }
    }

    private fun connectToDefaultDatabase() {
        val host = System.getenv("DB_HOST")
        val port = System.getenv("DB_PORT")
        val dbName = System.getenv("MYSQL_DATABASE")
        val username = System.getenv("MYSQL_USER")
        val password = Files.readString(Paths.get("/run/secrets/db_password")).trim()

        connectToDatabaseWith(DatabaseCredentials(host, port, dbName, username, password))
    }

    private fun connectToDatabaseWith(credentials: DatabaseCredentials) {
        listOf(userSQLRepository, friendshipRepository, friendshipRequestRepository).forEach {
            it.connect(credentials.host, credentials.port, credentials.dbName, credentials.username, credentials.password)
        }
    }

    private fun removeFriendship(userBlocking: User, userToBlock: User): Friendship {
        val friendshipToRemove = Friendship.of(userBlocking, userToBlock)
        friendshipService.deleteById(friendshipToRemove.id)
        return friendshipToRemove
    }

    private fun removeFriendshipRequest(userBlocking: User, userToBlock: User): FriendshipRequest {
        val friendshipRequestToRemove = FriendshipRequest.of(userBlocking, userToBlock)
        friendshipRequestService.deleteById(friendshipRequestToRemove.id)
        return friendshipRequestToRemove
    }

    private fun produceFriendshipRejectedEvent(producer: KafkaProducer<String, String>, friendshipRequestToRemove: FriendshipRequest) {
        val friendshipRequestRecord = KafkaProducerRecord.create<String, String>(
            FriendshipRequestRejected.TOPIC,
            mapper.writeValueAsString(friendshipRequestToRemove)
        )
        producer.write(friendshipRequestRecord)
    }

    private fun produceFriendshipRemovedEvent(producer: KafkaProducer<String, String>, friendshipToRemove: Friendship) {
        val friendshipToRemoveRecord = KafkaProducerRecord.create<String, String>(
            FriendshipRemoved.TOPIC,
            mapper.writeValueAsString(friendshipToRemove)
        )
        producer.write(friendshipToRemoveRecord)
    }
}
