package social.notification.infrastructure.controller.event

import io.vertx.core.AbstractVerticle
import io.vertx.kafka.client.consumer.KafkaConsumer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import social.common.events.AuthKeyGenerated
import social.common.events.FriendshipRequestAccepted
import social.common.events.FriendshipRequestRejected
import social.common.events.FriendshipRequestSent
import social.common.events.MessageSent
import social.notification.infrastructure.controller.socket.SocketHandlers
import social.notification.infrastructure.serialization.jackson.Mapper
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

class KafkaConsumerVerticle : AbstractVerticle() {
    private val logger: Logger = LogManager.getLogger(this::class)
    private val consumerConfig = mapOf(
        "bootstrap.servers" to (System.getenv("KAFKA_HOST") ?: "localhost") + ":" + (System.getenv("KAFKA_PORT") ?: "9092"),
        "key.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "value.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "group.id" to (System.getenv("GROUP_ID") ?: "notification-service"),
        "auto.offset.reset" to "earliest"
    )

    override fun start() {
        KafkaConsumer.create<String, String>(vertx, consumerConfig).apply {
            logger.info("subscribing")
            handler {
                logger.info("received an event topic={}", it.topic().toString())
                when (it.topic()) {
                    AuthKeyGenerated.TOPIC -> {
                        val event = Mapper.readValue(it.value(), AuthKeyGenerated::class.java)
                        SocketHandlers.publicKey.set(
                            KeyFactory.getInstance("RSA")
                                .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(event.publicKey)))
                        )
                    }
                    MessageSent.TOPIC -> {
                        val event = Mapper.readValue(it.value(), MessageSent::class.java)
                        SocketHandlers.send(event, event.receiver)
                    }
                    FriendshipRequestSent.TOPIC -> {
                        val event = Mapper.readValue(it.value(), FriendshipRequestSent::class.java)
                        SocketHandlers.send(event, event.receiver)
                    }
                    FriendshipRequestAccepted.TOPIC -> {
                        val event = Mapper.readValue(it.value(), FriendshipRequestAccepted::class.java)
                        SocketHandlers.send(event, event.receiver)
                    }
                    FriendshipRequestRejected.TOPIC -> {
                        val event = Mapper.readValue(it.value(), FriendshipRequestRejected::class.java)
                        SocketHandlers.send(event, event.receiver)
                    }
                }
            }
            val topics = setOf(
                AuthKeyGenerated.TOPIC,
                MessageSent.TOPIC,
                FriendshipRequestSent.TOPIC,
                FriendshipRequestAccepted.TOPIC,
                FriendshipRequestRejected.TOPIC,
            )
            subscribe(topics).onComplete {
                if (it.succeeded()) {
                    logger.info("subscribed")
                } else {
                    logger.error(it.cause())
                }
            }
        }
    }
}
