package social.friendship

import io.vertx.core.Vertx
import social.friendship.infrastructure.controller.rest.RESTFriendshipAPIVerticle

fun main(args: Array<String>) {
    val vertx: Vertx = Vertx.vertx()

    val api = RESTFriendshipAPIVerticle()

    vertx.deployVerticle(api)
}
