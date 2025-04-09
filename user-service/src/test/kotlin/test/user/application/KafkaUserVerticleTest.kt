package test.user.application

import io.vertx.core.AbstractVerticle
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.kafka.client.consumer.KafkaConsumer
import io.vertx.kafka.client.consumer.KafkaConsumerRecord
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertAll
import social.common.events.UserCreated
import social.common.events.UserUpdated
import social.user.application.UserServiceImpl
import social.user.domain.User
import social.user.infrastructure.controller.event.KafkaUserProducerVerticle
import social.user.infrastructure.persitence.sql.UserSQLRepository
import social.utils.docker.DockerTest
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class KafkaUserVerticleTest : DockerTest() {
    private val logger = LogManager.getLogger(this::class.java)
    private val user1 = User.of("test@gmail.com", "user1")
    private val userRepository = UserSQLRepository()
    private val dockerComposePath = "/social/user/application/docker-compose.yml"
    private val vertx = Vertx.vertx()
    private lateinit var dockerComposeFile: File
    private lateinit var producer: KafkaUserProducerVerticle
    private lateinit var consumer: KafkaUserConsumerVerticleTestClass
    private lateinit var service: UserServiceImpl

    @BeforeEach
    fun setUp() {
        val dockerComposeResource = this::class.java.getResource(dockerComposePath) ?: throw Exception("Resource not found")
        dockerComposeFile = File(dockerComposeResource.toURI())
        executeDockerComposeCmd(dockerComposeFile, "up", "--wait")

        userRepository.connect("127.0.0.1", "3306", "user", "test_user", "password")
        producer = KafkaUserProducerVerticle()
        service = UserServiceImpl(userRepository, producer)
        consumer = KafkaUserConsumerVerticleTestClass()
        deployVerticle(vertx, producer, consumer)
    }

    private fun deployVerticle(vertx: Vertx, vararg verticles: Verticle) {
        val latch = CountDownLatch(verticles.size)
        verticles.forEach { verticle ->
            vertx.deployVerticle(verticle).onComplete {
                latch.countDown()
                if (it.succeeded()) {
                    logger.info("Verticle '{}' started", verticle.javaClass.simpleName)
                } else {
                    logger.error("Failed to start verticle '{}':", verticle.javaClass.simpleName, it.cause())
                }
            }
        }
        latch.await()
    }

    @AfterEach
    fun tearDown() {
        executeDockerComposeCmd(dockerComposeFile, "down", "-v")
        closeVertxInstance()
    }

    private fun KafkaUserVerticleTest.closeVertxInstance() {
        vertx.close().onComplete {
            if (it.succeeded()) {
                logger.info("Vertx closed")
            } else {
                logger.error("Failed to close Vertx", it.cause())
            }
        }
    }

    @Timeout(5 * 60)
    @Test
    fun publishesEventUserCreated() {
        val before = userRepository.findById(user1.id)
        deployVerticle(vertx, consumer)

        // waits for the event to be processed
        val latch = CountDownLatch(1)
        lateinit var after: User
        vertx.eventBus().consumer<String>(UserCreated.TOPIC) {
            userRepository.findById(user1.id)?.let {
                after = it
                latch.countDown()
            } ?: run {
                logger.error("User not found")
                latch.countDown()
            }
        }
        service.addUser(user1)
        latch.await(4, TimeUnit.MINUTES)

        assertAll(
            { assertEquals(null, before) },
            { assertEquals(user1, after) }
        )
    }
}

/**
 * Class defined only to test events that this microservice produces, in order to test its producer.
 */
class KafkaUserConsumerVerticleTestClass : AbstractVerticle() {
    private val logger = LogManager.getLogger(this::class)
    private val consumerConfig = mapOf(
        "bootstrap.servers" to (System.getenv("KAFKA_HOST") ?: "localhost") + ":" + (System.getenv("KAFKA_PORT") ?: "9092"),
        "key.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "value.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "group.id" to "user-service",
        "auto.offset.reset" to "earliest"
    )
    private val events: MutableSet<String> = mutableSetOf(
        UserCreated.TOPIC,
        UserUpdated.TOPIC,
    )
    private lateinit var consumer: KafkaConsumer<String, String>

    /**
     * Starts the Kafka consumer.
     */
    override fun start() {
        consumer = KafkaConsumer.create(vertx, consumerConfig)
        subscribeToEvents()
        handleEvents()
    }

    /**
     * Subscribes to the events that this microservice is interested in.
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
     * Handles the events received from the Kafka consumer.
     */
    private fun handleEvents() {
        consumer.handler { record ->
            logger.trace("Received event: TOPIC:{}, KEY:{}, VALUE:{}", record.topic(), record.key(), record.value())
            when (record.topic()) {
                UserCreated.TOPIC -> userCreatedHandler(record)
                UserUpdated.TOPIC -> userUpdatedHandler(record)
                else -> logger.warn("Received event from unknown topic: {}", record.topic())
            }
        }
    }

    /**
     * Simply forwards the event to the event bus to be able to check whether the test has been successful.
     */
    private fun userCreatedHandler(record: KafkaConsumerRecord<String, String>) {
        vertx.eventBus().publish(UserCreated.TOPIC, record.value())
    }

    /**
     * Simply forwards the event to the event bus to be able to check whether the test has been successful.
     */
    private fun userUpdatedHandler(record: KafkaConsumerRecord<String, String>) {
        vertx.eventBus().publish(UserUpdated.TOPIC, record.value())
    }
}
