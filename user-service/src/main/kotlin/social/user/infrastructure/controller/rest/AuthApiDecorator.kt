package social.user.infrastructure.controller.rest

import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import social.common.endpoint.Endpoint
import social.common.endpoint.StatusCode
import social.user.application.AuthService
import social.user.application.UserService
import social.user.domain.Credentials
import social.user.infrastructure.serialization.jackson.Mapper
import java.util.concurrent.Callable

class AuthApiDecorator(
    userService: UserService,
    private val authService: AuthService
) : UserApiVerticle(userService) {

    override fun addEndPoints(router: Router) {
        super.addEndPoints(router)
        router.post(Endpoint.LOGIN).handler(::login)
        router.post(Endpoint.CREDENTIALS).handler(::addCredentials)
    }

    private fun login(context: RoutingContext) {
        this.context.executeBlocking(
            Callable {
                val requestBody = context.body().asString()
                    ?: throw IllegalArgumentException("Request body is missing")
                val credentials = Mapper.readValue(requestBody, Credentials::class.java)
                authService.login(credentials)
            }
        ).onComplete(respond(context, StatusCode.OK))
    }

    private fun addCredentials(context: RoutingContext) {
        this.context.executeBlocking(
            Callable {
                val requestBody = context.body().asString()
                    ?: throw IllegalArgumentException("Request body is missing")
                val credentials = Mapper.readValue(requestBody, Credentials::class.java)
                authService.addCredentials(credentials)
            }
        ).onComplete(respond(context, StatusCode.CREATED))
    }
}
