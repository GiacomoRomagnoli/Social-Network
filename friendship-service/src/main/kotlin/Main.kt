package social.friendship

import io.vertx.core.Verticle
import io.vertx.core.Vertx
import social.friendship.infrastructure.controller.event.KafkaFriendshipEventManagerVerticle
import social.friendship.infrastructure.controller.rest.RESTFriendshipAPIVerticle

fun main(args: Array<String>) {
    val vertx: Vertx = Vertx.vertx()

    val api = RESTFriendshipAPIVerticle()
    val kafka = KafkaFriendshipEventManagerVerticle()

    deployVerticles(vertx, api, kafka)
}

private fun deployVerticles(vertx: Vertx, vararg verticles: Verticle) {
    verticles.forEach {
        vertx.deployVerticle(it)
    }
}
