package social.friendship.infrastructure.controller.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vertx.core.AbstractVerticle
import io.vertx.kafka.client.consumer.KafkaConsumer
import io.vertx.kafka.client.consumer.KafkaConsumerRecord
import org.apache.logging.log4j.LogManager
import social.common.events.UserCreated
import social.friendship.social.friendship.domain.User
import social.friendship.social.friendship.domain.application.FriendshipService
import java.util.concurrent.Callable

class KafkaFriendshipConsumerVerticle(private val service: FriendshipService) : AbstractVerticle() {
    private val logger = LogManager.getLogger(this::class)
    private val consumerConfig = mapOf(
        "bootstrap.servers" to "localhost:9092",
        "key.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "value.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "group.id" to "friendship-service",
        "auto.offset.reset" to "earliest"
    )
    private val mapper: ObjectMapper = jacksonObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
    }
    private val events: MutableSet<String> = mutableSetOf(
        UserCreated.TOPIC,
    )
    private lateinit var consumer: KafkaConsumer<String, String>

    override fun start() {
        consumer = KafkaConsumer.create(vertx, consumerConfig)
        subscribeToEvents()
        handleEvents()
    }

    private fun subscribeToEvents() {
        consumer.subscribe(events) { result ->
            if (result.succeeded()) {
                logger.debug("Subscribed to events: {}", events)
            } else {
                logger.error("Failed to subscribe to events {}", events)
            }
        }
    }

    private fun handleEvents() {
        consumer.handler { record ->
            logger.trace("Received event: TOPIC:{}, KEY:{}, VALUE:{}", record.topic(), record.key(), record.value())
            when (record.topic()) {
                UserCreated.TOPIC -> userCreatedHandler(record)
                else -> logger.warn("Received event from unknown topic: {}", record.topic())
            }
        }
    }

    private fun userCreatedHandler(record: KafkaConsumerRecord<String, String>) {
        vertx.executeBlocking(
            Callable {
                val userCreatedEventData = mapper.readValue(record.value(), UserCreated::class.java)
                val user = User.of(userCreatedEventData.email)
                service.addUser(user)
            }
        ).onComplete { result ->
            if (result.succeeded()) {
                logger.trace("User created event processed")
            } else {
                logger.error("Failed to process user created event", result.cause())
            }
        }
    }
}
