package social.notification

import io.vertx.core.Vertx
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import social.notification.infrastructure.controller.event.KafkaConsumerVerticle
import social.notification.infrastructure.controller.socket.NotificationVerticle

fun main() {
    val logger: Logger = LogManager.getLogger("Main")
    val vertx = Vertx.vertx()
    val consumer = KafkaConsumerVerticle()
    val server = NotificationVerticle()
    vertx.deployVerticle(consumer).onSuccess {
        vertx.deployVerticle(server).onSuccess {
            logger.info("Successfully deployed server")
        }.onFailure {
            logger.error(it.message)
        }
    }.onFailure {
        logger.error(it.message)
    }
}
