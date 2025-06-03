package social.gateway.infrastructure.controller.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vertx.core.AbstractVerticle
import io.vertx.kafka.client.consumer.KafkaConsumer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import social.common.events.AuthKeyGenerated
import social.gateway.infrastructure.controller.rest.AuthHandlers
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

class KafkaConsumerVerticle : AbstractVerticle() {
    private val logger: Logger = LogManager.getLogger(this::class)
    private val consumerConfig = mapOf(
        "bootstrap.servers" to (System.getenv("KAFKA_HOST") ?: "localhost") + ":" + (System.getenv("KAFKA_PORT") ?: "9092"),
        "key.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "value.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "group.id" to (System.getenv("GROUP_ID") ?: "api-gateway"),
        "auto.offset.reset" to "earliest"
    )
    private val mapper: ObjectMapper = jacksonObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
    }

    override fun start() {
        KafkaConsumer.create<String, String>(vertx, consumerConfig).apply {
            logger.trace("subscribing")
            handler {
                when (it.topic()) {
                    AuthKeyGenerated.TOPIC -> {
                        logger.trace("received an event topic={}", it.topic().toString())
                        val event = mapper.readValue(it.value(), AuthKeyGenerated::class.java)
                        AuthHandlers.publicKey.set(
                            KeyFactory.getInstance("RSA")
                                .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(event.publicKey)))
                        )
                    }
                }
            }
            subscribe(AuthKeyGenerated.TOPIC).onComplete {
                if (it.succeeded()) {
                    logger.trace("subscribed")
                } else {
                    logger.error(it.cause())
                }
            }
        }
    }
}
