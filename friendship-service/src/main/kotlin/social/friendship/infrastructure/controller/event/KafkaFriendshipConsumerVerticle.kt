package social.friendship.infrastructure.controller.event

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Verticle
import io.vertx.kafka.client.consumer.KafkaConsumer
import io.vertx.kafka.client.consumer.KafkaConsumerRecord
import org.apache.logging.log4j.LogManager
import social.common.events.UserCreated
import social.friendship.application.FriendshipService
import social.friendship.domain.User
import social.friendship.infrastructure.probes.AsyncProbe
import social.friendship.social.friendship.infrastructure.serialization.jackson.Mapper
import java.util.concurrent.Callable

/**
 * Interface for a verticle that consumes events from Kafka
 */
interface KafkaConsumerVerticle : Verticle

/**
 * Verticle that consumes events from Kafka
 * @param service the friendship service
 */
class KafkaFriendshipConsumerVerticle(
    private val service: FriendshipService
) : AbstractVerticle(), KafkaConsumerVerticle, AsyncProbe {
    private val logger = LogManager.getLogger(this::class)
    private val consumerConfig = mapOf(
        "bootstrap.servers" to (System.getenv("KAFKA_HOST") ?: "localhost") + ":" + (System.getenv("KAFKA_PORT") ?: "9092"),
        "key.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "value.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "group.id" to "friendship-service",
        "auto.offset.reset" to "earliest"
    )
    private val events: MutableSet<String> = mutableSetOf(
        UserCreated.TOPIC,
    )
    private lateinit var consumer: KafkaConsumer<String, String>

    /**
     * Start the consumer and subscribe to events
     */
    override fun start() {
        consumer = KafkaConsumer.create(vertx, consumerConfig)
        subscribeToEvents()
        handleEvents()
    }

    /**
     * Subscribe to events
     */
    private fun subscribeToEvents() {
        consumer.subscribe(events) { result ->
            if (result.succeeded()) {
                logger.debug("Subscribed to events: {}", events)
            } else {
                logger.error("Failed to subscribe to events {}", events)
            }
        }
    }

    /**
     * Handle events
     */
    private fun handleEvents() {
        consumer.handler { record ->
            logger.trace("Received event: TOPIC:{}, KEY:{}, VALUE:{}", record.topic(), record.key(), record.value())
            when (record.topic()) {
                UserCreated.TOPIC -> userCreatedHandler(record)
                else -> logger.warn("Received event from unknown topic: {}", record.topic())
            }
        }
    }

    /**
     * Handle user created event
     * @param record the Kafka record
     */
    private fun userCreatedHandler(record: KafkaConsumerRecord<String, String>) {
        this.context.executeBlocking(
            Callable {
                val userCreatedEventData = Mapper.readValue(record.value(), UserCreated::class.java)
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

    override fun isReady(): Future<Unit> =
        consumer.partitionsFor("health-check").mapEmpty()
}
