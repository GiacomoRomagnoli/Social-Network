package social.gateway.infrastructure.controller.rest

import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.WebClient
import org.apache.logging.log4j.LogManager
import social.common.endpoint.Endpoint
import social.common.endpoint.Port
import social.gateway.infrastructure.controller.rest.Utils.forwardResponse
import social.gateway.infrastructure.controller.rest.Utils.sendUnauthorizedResponse

object FriendshipHandlers {
    private const val FRIENDSHIP_SERVICE = "friendship-service"
    private val logger = LogManager.getLogger(this::class.java)

    fun getFriendships(context: RoutingContext, webClient: WebClient) {
        val email = context.pathParam(Endpoint.EMAIL_PARAM)
        if (email == context.get(AuthHandlers.USER_ID)) {
            webClient
                .get(Port.HTTP, FRIENDSHIP_SERVICE, Endpoint.FRIENDSHIP)
                .addQueryParam("id", email)
                .send()
                .onComplete(forwardResponse(context))
            return
        }
        sendUnauthorizedResponse(context)
    }

    fun getFriendshipRequests(context: RoutingContext, webClient: WebClient) {
        val email = context.pathParam(Endpoint.EMAIL_PARAM)
        if (email == context.get(AuthHandlers.USER_ID)) {
            webClient
                .get(Port.HTTP, FRIENDSHIP_SERVICE, Endpoint.FRIENDSHIP_REQUEST)
                .addQueryParam("id", email)
                .send()
                .onComplete(forwardResponse(context))
            return
        }
        sendUnauthorizedResponse(context)
    }

    fun postFriendshipRequest(context: RoutingContext, webClient: WebClient) {
        val body = context.body().asJsonObject()
        val email = body.getString("from")
        if (email == context.get(AuthHandlers.USER_ID)) {
            webClient
                .post(Port.HTTP, FRIENDSHIP_SERVICE, Endpoint.FRIENDSHIP_REQUEST_SEND)
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(body)
                .onComplete(forwardResponse(context))
            return
        }
        sendUnauthorizedResponse(context)
    }

    fun acceptFriendshipRequest(context: RoutingContext, webClient: WebClient) {
        val body = context.body().asJsonObject()
        val email = body.getString("to")
        if (email == context.get(AuthHandlers.USER_ID)) {
            webClient
                .put(Port.HTTP, FRIENDSHIP_SERVICE, Endpoint.FRIENDSHIP_REQUEST_ACCEPT)
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(body)
                .onComplete(forwardResponse(context))
            return
        }
        sendUnauthorizedResponse(context)
    }

    fun declineFriendshipRequest(context: RoutingContext, webClient: WebClient) {
        val body = context.body().asJsonObject()
        val email = body.getString("to")
        if (email == context.get(AuthHandlers.USER_ID)) {
            webClient
                .put(Port.HTTP, FRIENDSHIP_SERVICE, Endpoint.FRIENDSHIP_REQUEST_DECLINE)
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(body)
                .onComplete(forwardResponse(context))
            return
        }
        sendUnauthorizedResponse(context)
    }
}
