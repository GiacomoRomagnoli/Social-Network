package social.friendship.infrastructure.controller.event

import io.vertx.core.Vertx
import io.vertx.kafka.client.producer.KafkaProducer

object KafkaFriendshipProducer {
    private val producerConfig = mapOf(
        "bootstrap.servers" to "localhost:9092",
        "key.serializer" to "org.apache.kafka.common.serialization.StringSerializer",
        "value.serializer" to "org.apache.kafka.common.serialization.StringSerializer",
        "acks" to "1"
    )

    fun createProducer(vertx: Vertx): KafkaProducer<String, String> {
        return KafkaProducer.create(vertx, producerConfig)
    }
}
