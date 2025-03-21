package social.user.infrastructure.controller.rest

import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import social.common.endpoint.Endpoint
import social.common.endpoint.StatusCode
import social.user.application.AuthService
import social.user.application.UserService
import social.user.domain.Credentials
import social.user.domain.User
import java.util.concurrent.Callable

class HttpServerVerticle(
    userService: UserService,
    private val authService: AuthService
) : RESTUserAPIVerticle(userService) {

    override fun addEndPoints(router: Router) {
        super.addEndPoints(router)
        router.post(Endpoint.LOGIN).handler(::login)
        router.post(Endpoint.CREDENTIALS).handler(::addCredentials)
        router.delete(Endpoint.USER_EMAIL_PARAM).handler(::deleteUser)
    }

    private fun login(context: RoutingContext) {
        vertx.executeBlocking(
            Callable {
                val requestBody = context.body().asJsonObject()
                val email = requestBody.getString("email")
                    ?: throw IllegalArgumentException("Email is required")
                val password = requestBody.getString("password")
                    ?: throw IllegalArgumentException("Password is required")
                authService.login(Credentials.of(email, password, false))
            }
        ).onComplete {
            if (it.succeeded()) {
                sendResponse(context, StatusCode.OK, it.result())
            } else {
                sendErrorResponse(context, it.cause())
            }
        }
    }

    private fun addCredentials(context: RoutingContext) {
        vertx.executeBlocking(
            Callable {
                val requestBody = context.body().asJsonObject()
                val email = requestBody.getString("email")
                    ?: throw IllegalArgumentException("Email is required")
                val password = requestBody.getString("password")
                    ?: throw IllegalArgumentException("Password is required")
                authService.addCredentials(Credentials.of(email, password))
            }
        ).onComplete {
            if (it.succeeded()) {
                sendResponse(context, StatusCode.CREATED)
            } else {
                sendErrorResponse(context, it.cause())
            }
        }
    }

    private fun deleteUser(context: RoutingContext) {
        vertx.executeBlocking(
            Callable {
                val email = context.pathParam("email")
                    ?: throw IllegalArgumentException("Email is required")
                service.deleteUser(User.userIDOf(email))
                    ?: throw IllegalStateException("User not found")
            }
        ).onComplete {
            if (it.succeeded()) {
                sendResponse(context, StatusCode.NO_CONTENT)
            } else {
                sendErrorResponse(context, it.cause())
            }
        }
    }
}
