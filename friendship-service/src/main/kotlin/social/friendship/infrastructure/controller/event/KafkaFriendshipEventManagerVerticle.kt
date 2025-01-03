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
import social.friendship.social.friendship.infrastructure.persistence.sql.FriendshipRequestSQLRepository
import social.friendship.social.friendship.infrastructure.persistence.sql.FriendshipSQLRepository
import social.friendship.social.friendship.infrastructure.persistence.sql.UserSQLRepository

class KafkaFriendshipEventManagerVerticle : AbstractVerticle() {
    private val logger = LogManager.getLogger(this::class)
    private val userService: FriendshipService<UserID, User> = FriendshipServiceImpl(UserSQLRepository())
    private val friendshipService: FriendshipService<FriendshipID, Friendship> = FriendshipServiceImpl(FriendshipSQLRepository())
    private val friendshipRequestService: FriendshipService<FriendshipRequestID, FriendshipRequest> = FriendshipServiceImpl(FriendshipRequestSQLRepository())
    private val consumer: KafkaConsumer<String, String> = KafkaFriendshipConsumer.createConsumer(vertx)
    private val producer: KafkaProducer<String, String> = KafkaFriendshipProducer.createProducer(vertx)
    private val events: MutableSet<String> = mutableSetOf(
        UserCreated.TOPIC,
        UserBlocked.TOPIC
    )
    private val mapper: ObjectMapper = jacksonObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
    }

    override fun start() {
        consumer.subscribe(events) { result ->
            if (result.succeeded()) {
                logger.debug("Subscribed to events: {}", events)
            } else {
                logger.error("Failed to subscribe to events {}", events)
            }
        }

        consumer.handler { record ->
            logger.debug("Received event: {}", record.value())
            when (record.topic()) {
                UserCreated.TOPIC -> {
                    mapper.readValue(record.value(), User::class.java).let { user ->
                        userService.add(user)
                    }
                }

                UserBlocked.TOPIC -> {
                    val usersPairTypeRef = object : TypeReference<Pair<User, User>>() {}
                    mapper.readValue(record.value(), usersPairTypeRef).let { (userBlocking, userToBlock) ->
                        val friendshipRequestToRemove = removeFriendshipRequest(userBlocking, userToBlock)
                        produceFriendshipRejectedEvent(friendshipRequestToRemove)

                        val friendshipToRemove = removeFriendship(friendshipRequestToRemove)
                        produceFriendshipRemovedEvent(friendshipToRemove)
                    }
                }
            }
        }
    }

    private fun removeFriendshipRequest(
        userBlocking: User,
        userToBlock: User
    ): FriendshipRequest {
        val friendshipRequestToRemove = FriendshipRequest.of(userBlocking, userToBlock)
        friendshipRequestService.deleteById(friendshipRequestToRemove.id)
        return friendshipRequestToRemove
    }

    private fun produceFriendshipRejectedEvent(friendshipRequestToRemove: FriendshipRequest) {
        val friendshipRequestRecord = KafkaProducerRecord.create<String, String>(
            FriendshipRequestRejected.TOPIC,
            mapper.writeValueAsString(friendshipRequestToRemove)
        )
        producer.write(friendshipRequestRecord)
    }

    private fun removeFriendship(friendshipRequestToRemove: FriendshipRequest): Friendship {
        val friendshipToRemove = Friendship.of(friendshipRequestToRemove)
        friendshipService.deleteById(friendshipToRemove.id)
        return friendshipToRemove
    }

    private fun produceFriendshipRemovedEvent(friendshipToRemove: Friendship) {
        val friendshipToRemoveRecord = KafkaProducerRecord.create<String, String>(
            FriendshipRemoved.TOPIC,
            mapper.writeValueAsString(friendshipToRemove)
        )
        producer.write(friendshipToRemoveRecord)
    }
}
