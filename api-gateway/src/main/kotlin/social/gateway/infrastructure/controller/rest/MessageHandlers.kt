package social.gateway.infrastructure.controller.rest

import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.WebClient
import social.common.endpoint.Endpoint
import social.common.endpoint.Port
import social.common.endpoint.StatusCode
import social.gateway.infrastructure.controller.rest.Utils.forwardResponse
import social.gateway.infrastructure.controller.rest.Utils.sendServiceUnavailableResponse
import social.gateway.infrastructure.controller.rest.Utils.sendUnauthorizedResponse

object MessageHandlers {
    private const val MESSAGE_SERVICE = "friendship-service"

    fun sendMessage(context: RoutingContext, webClient: WebClient) {
        val body = context.body().asJsonObject()
        val email = body.getJsonObject("sender")
            .getJsonObject("userId")
            .getString("value")
        if (email == context.get(AuthHandlers.USER_ID)) {
            webClient
                .post(Port.HTTP, MESSAGE_SERVICE, Endpoint.MESSAGE_SEND)
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(body)
                .onComplete(forwardResponse(context))
            return
        }
        sendUnauthorizedResponse(context)
    }

    fun getChat(context: RoutingContext, webClient: WebClient) {
        val user1 = context.pathParam(Endpoint.USER_PARAM_1)
        val user2 = context.pathParam(Endpoint.USER_PARAM_2)
        val id = context.get<String>(AuthHandlers.USER_ID)
        if (user1 == id || user2 == id) {
            webClient
                .get(Port.HTTP, MESSAGE_SERVICE, Endpoint.MESSAGE_CHAT)
                .addQueryParam("user1Id", user1)
                .addQueryParam("user2Id", user2)
                .send()
                .onComplete(forwardResponse(context))
            return
        }
        sendUnauthorizedResponse(context)
    }

    fun getMessage(context: RoutingContext, webClient: WebClient) {
        val user = context.get<String>(AuthHandlers.USER_ID)
        webClient
            .get(Port.HTTP, MESSAGE_SERVICE, context.request().uri())
            .send()
            .onComplete {
                if (it.succeeded()) {
                    val body = it.result().bodyAsJsonObject()
                    val sender = body.getJsonObject("sender")
                        .getJsonObject("userId")
                        .getString("value")
                    val receiver = body.getJsonObject("receiver")
                        .getJsonObject("userId")
                        .getString("value")
                    if (user == receiver || user == sender) {
                        context.response()
                            .putHeader("Content-Type", "application/json")
                            .setStatusCode(StatusCode.OK)
                            .end(it.result().body())
                        return@onComplete
                    }
                    sendUnauthorizedResponse(context)
                    return@onComplete
                }
                sendServiceUnavailableResponse(context)
            }
    }
}
