package social.friendship

import io.vertx.core.Vertx
import social.friendship.application.FriendshipServiceImpl
import social.friendship.infrastructure.controller.event.KafkaFriendshipConsumerVerticle
import social.friendship.infrastructure.controller.event.KafkaFriendshipProducerVerticle
import social.friendship.infrastructure.controller.rest.RESTFriendshipAPIVerticleImpl
import social.friendship.infrastructure.persistence.sql.FriendshipRequestSQLRepository
import social.friendship.infrastructure.persistence.sql.FriendshipSQLRepository
import social.friendship.infrastructure.persistence.sql.MessageSQLRepository
import social.friendship.infrastructure.persistence.sql.UserSQLRepository

fun main(args: Array<String>) {
    val vertx: Vertx = Vertx.vertx()

    val userRepository = UserSQLRepository()
    val friendshipRepository = FriendshipSQLRepository()
    val friendshipRequestRepository = FriendshipRequestSQLRepository()
    val messageRepository = MessageSQLRepository()
    val producer = KafkaFriendshipProducerVerticle()
    val service = FriendshipServiceImpl(
        userRepository,
        friendshipRepository,
        friendshipRequestRepository,
        messageRepository,
        producer,
    )
    val api = RESTFriendshipAPIVerticleImpl(service)
    val consumer = KafkaFriendshipConsumerVerticle(service)
    vertx.deployVerticle(producer).onSuccess {
        vertx.deployVerticle(consumer).onSuccess {
            vertx.deployVerticle(api)
        }
    }
}
