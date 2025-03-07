package social.user.infrastructure.controller.rest

import io.vertx.core.http.HttpServerOptions
import io.vertx.core.net.JksOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import social.common.endpoint.Endpoint
import social.common.endpoint.StatusCode
import social.user.application.AuthService
import social.user.application.UserService
import social.user.domain.Credentials
import java.util.concurrent.Callable

class HttpsServerVerticle(
    userService: UserService,
    private val authService: AuthService
) : RESTUserAPIVerticle(userService) {

    override fun addEndPoints(router: Router) {
        super.addEndPoints(router)
        router.post(Endpoint.LOGIN).handler(::login)
    }

    override fun options(): HttpServerOptions =
        HttpServerOptions()
            .setSsl(true)
            .setKeyStoreOptions(
                JksOptions()
                    .setPath("src/main/resources/keystore.jks")
                    .setPassword("secret")
            )

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
}
