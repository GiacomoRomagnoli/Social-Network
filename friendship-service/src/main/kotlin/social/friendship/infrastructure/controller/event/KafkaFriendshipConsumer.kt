package social.friendship.infrastructure.controller.event

import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.kafka.client.consumer.KafkaConsumer

object KafkaFriendshipConsumer : AbstractVerticle() {
    val consumerConfig = mapOf(
        "bootstrap.servers" to "localhost:9092",
        "key.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "value.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "group.id" to "friendship-service",
        "auto.offset.reset" to "earliest"
    )

    fun createConsumer(vertx: Vertx): KafkaConsumer<String, String> {
        return KafkaConsumer.create(vertx, consumerConfig)
    }
}
