package social.gateway.infrastructure.controller.rest

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.WebClient
import social.common.endpoint.Endpoint
import social.common.endpoint.Port
import social.common.endpoint.StatusCode
import social.gateway.infrastructure.controller.rest.Utils.forwardResponse
import social.gateway.infrastructure.controller.rest.Utils.sendServiceUnavailableResponse
import social.gateway.infrastructure.controller.rest.Utils.sendUnauthorizedResponse

object UserHandlers {
    private const val USER_SERVICE = "user-service"

    fun login(context: RoutingContext, webClient: WebClient) {
        webClient
            .post(Port.HTTP, USER_SERVICE, Endpoint.LOGIN)
            .putHeader("Content-Type", "application/json")
            .sendJsonObject(context.body().asJsonObject())
            .onComplete(forwardResponse(context))
    }

    fun getUser(context: RoutingContext, webClient: WebClient) {
        val email = context.pathParam(Endpoint.EMAIL_PARAM)
        if (email == context.get(AuthHandlers.USER_ID)) {
            webClient
                .get(Port.HTTP, USER_SERVICE, Endpoint.USER)
                .addQueryParam(Endpoint.EMAIL_PARAM, email)
                .send()
                .onComplete(forwardResponse(context))
            return
        }
        sendUnauthorizedResponse(context)
    }

    fun putUser(context: RoutingContext, webClient: WebClient) {
        val body = context.body().asJsonObject()
        val email = body.getString("email")
        if (email == context.get(AuthHandlers.USER_ID)) {
            webClient
                .put(Port.HTTP, USER_SERVICE, Endpoint.USER)
                .sendJsonObject(body)
                .onComplete(forwardResponse(context))
            return
        }
        sendUnauthorizedResponse(context)
    }

    fun postUser(context: RoutingContext, webClient: WebClient) {
        val body = context.body().asJsonObject()
        val email = body.getString("email")
        val username = body.getString("username")
        val password = body.getString("password")
        if (email == null || username == null || password == null) {
            context.response()
                .setStatusCode(StatusCode.BAD_REQUEST)
                .end("email, username and password must be provided")
            return
        }
        val user = JsonObject().put("email", email).put("username", username)
        val credentials = JsonObject().put("email", email).put("password", password)
        webClient
            .post(Port.HTTP, USER_SERVICE, Endpoint.USER)
            .putHeader("Content-Type", "application/json")
            .sendJsonObject(user)
            .onComplete step1@{ req1 ->
                if (req1.succeeded()) {
                    if (req1.result().statusCode() == StatusCode.CREATED) {
                        webClient
                            .post(Port.HTTP, USER_SERVICE, Endpoint.CREDENTIALS)
                            .putHeader("Content-Type", "application/json")
                            .sendJsonObject(credentials)
                            .onComplete step2@{ req2 ->
                                if (req2.succeeded()) {
                                    if (req2.result().statusCode() == StatusCode.CREATED) {
                                        context.response()
                                            .setStatusCode(StatusCode.CREATED)
                                            .end()
                                        return@step2
                                    }
                                    context.response()
                                        .setStatusCode(req2.result().statusCode())
                                        .end(req2.result().body())
                                } else {
                                    sendServiceUnavailableResponse(context)
                                }
                                webClient
                                    .delete(Port.HTTP, USER_SERVICE, "${Endpoint.USER}/$email")
                                    .send()
                            }
                    }
                    return@step1
                }
                sendServiceUnavailableResponse(context)
            }
    }
}
