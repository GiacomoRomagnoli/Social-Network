package social.gateway.infrastructure.controller.rest

import io.jsonwebtoken.Jwts
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.WebClient
import social.common.endpoint.Endpoint
import social.common.endpoint.Port
import social.common.endpoint.StatusCode
import java.security.PublicKey
import java.util.concurrent.atomic.AtomicReference

object UserHandlers {

    fun login(context: RoutingContext, webClient: WebClient) {
        webClient
            .post(Port.HTTP, "user-service", Endpoint.LOGIN)
            .putHeader("Content-Type", "application/json")
            .sendJsonObject(context.body().asJsonObject())
            .onComplete {
                context.response()
                    .setStatusCode(it.result().statusCode())
                    .end(it.result().body())
            }
    }

    fun getUser(context: RoutingContext, webClient: WebClient) {
        val email = context.pathParam(Endpoint.EMAIL_PARAM)
        if (email == context.get(AuthHandlers.USER_ID)) {
            webClient
                .get(Port.HTTP, "user-service", Endpoint.USER)
                .addQueryParam(Endpoint.EMAIL_PARAM, email)
                .send()
                .onComplete {
                    context.response()
                        .setStatusCode(it.result().statusCode())
                        .end(it.result().body())
                }
            return
        }
        context.response()
            .setStatusCode(StatusCode.UNAUTHORIZED)
            .end()
    }

    fun putUser(context: RoutingContext, webClient: WebClient) {
        val body = context.body().asJsonObject()
        val email = body.getString("email")
        if (email == context.get(AuthHandlers.USER_ID)) {
            webClient
                .put(Port.HTTP, "user-service", Endpoint.USER)
                .sendJsonObject(body)
                .onComplete {
                    context.response()
                        .setStatusCode(it.result().statusCode())
                        .end(it.result().body())
                }
            return
        }
        context.response()
            .setStatusCode(StatusCode.UNAUTHORIZED)
            .end()
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
        webClient
            .post(Port.HTTP, "user-service", Endpoint.USER)
            .putHeader("Content-Type", "application/json")
            .sendJsonObject(
                JsonObject()
                    .put("email", email)
                    .put("username", username)
            )
            .onComplete { req1 ->
                if (req1.succeeded() && req1.result().statusCode() == StatusCode.CREATED) {
                    webClient
                        .post(Port.HTTP, "user-service", Endpoint.CREDENTIALS)
                        .putHeader("Content-Type", "application/json")
                        .sendJsonObject(
                            JsonObject()
                                .put("email", email)
                                .put("password", password)
                        )
                        .onComplete { req2 ->
                            if (req2.succeeded() && req2.result().statusCode() == StatusCode.CREATED) {
                                context.response()
                                    .setStatusCode(StatusCode.CREATED)
                                    .end()
                            } else {
                                context.response()
                                    .setStatusCode(req2.result().statusCode())
                                    .end(req2.result().body())
                                webClient
                                    .delete(Port.HTTP, "user-service", "${Endpoint.USER}/$email")
                                    .send()
                            }
                        }
                } else {
                    context.response()
                        .setStatusCode(req1.result().statusCode())
                        .end(req1.result().body())
                }
            }
    }
}

object AuthHandlers {
    val publicKey = AtomicReference<PublicKey?>(null)
    internal const val USER_ID = "userId"

    fun jwtAuth(context: RoutingContext) {
        if (publicKey.get() == null) {
            context.response()
                .setStatusCode(StatusCode.SERVICE_UNAVAILABLE)
                .end("unable to authenticate")
            return
        }
        val authHeader = context.request().getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            context.response()
                .setStatusCode(StatusCode.UNAUTHORIZED)
                .end("missing token or malformed")
            return
        }
        try {
            val token = authHeader.removePrefix("Bearer ").trim()
            val claims = Jwts.parser().verifyWith(publicKey.get()).build().parseSignedClaims(token)
            context.put(USER_ID, claims.payload.subject)
            context.next()
        } catch (e: Exception) {
            context.response()
                .setStatusCode(StatusCode.UNAUTHORIZED)
                .end("invalid token: ${e.message}")
        }
    }
}
