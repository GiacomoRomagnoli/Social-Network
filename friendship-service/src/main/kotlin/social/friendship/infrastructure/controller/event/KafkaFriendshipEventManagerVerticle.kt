package social.friendship.infrastructure.controller.event

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vertx.core.AbstractVerticle
import io.vertx.kafka.client.consumer.KafkaConsumer
import io.vertx.kafka.client.consumer.KafkaConsumerRecord
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
            logger.debug("Received event: TOPIC:{}, KEY:{}, VALUE:{}", record.topic(), record.key(), record.value())
            when (record.topic()) {
                UserCreated.TOPIC -> addUser(record)

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

                else -> {
                    logger.warn("Received event from unknown topic: {}", record.topic())
                }
            }
        }
    }

    private fun addUser(record: KafkaConsumerRecord<String, String>) {
        fun addUserFromId() {
            val inferredUser = User.of(record.value())
            userService.add(inferredUser)
            vertx.eventBus().publish(UserCreated.TOPIC, mapper.writeValueAsString(inferredUser))
        }

        fun addUserFromJson() {
            mapper.readValue(record.value(), User::class.java).let {
                userService.add(it)
                vertx.eventBus().publish(UserCreated.TOPIC, record.value())
            }
        }

        logger.trace("Received UserCreated event: {}", record.value())
        try {
            if (record.value() != null) {
                logger.trace("Received UserCreated event with value: {}", record.value().toString())
                if (record.key() == "id") {
                    logger.trace("Received UserCreated event with ID: {}", record.key())
                    addUserFromId()
                } else {
                    logger.trace("Received UserCreated event with JSON: {}", record.value())
                    addUserFromJson()
                }
            } else {
                logger.warn("Received UserCreated event with null value, skipping event processing operation..")
                vertx.eventBus().publish(UserCreated.TOPIC, null)
            }
        } catch (e: Exception) {
            logger.error("Failed to process event: key:{}, value:{}", record.key(), record.value(), e)
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
