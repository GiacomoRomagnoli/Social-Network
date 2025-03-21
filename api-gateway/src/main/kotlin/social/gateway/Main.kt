package social.gateway

import io.vertx.core.Vertx
import social.gateway.infrastructure.controller.event.KafkaConsumerVerticle
import social.gateway.infrastructure.controller.rest.GatewayVerticle

fun main() {
    val vertx = Vertx.vertx()
    val consumer = KafkaConsumerVerticle()
    vertx.deployVerticle(consumer).onComplete {
        if (it.succeeded()) {
            val gateway = GatewayVerticle()
            vertx.deployVerticle(gateway)
        }
    }
}
