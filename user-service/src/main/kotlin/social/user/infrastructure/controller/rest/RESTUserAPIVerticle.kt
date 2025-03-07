package social.user.infrastructure.controller.rest

import io.vertx.core.AbstractVerticle
import io.vertx.core.Verticle
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import social.common.endpoint.Endpoint
import social.common.endpoint.Port
import social.common.endpoint.StatusCode
import social.user.application.UserService
import social.user.domain.User
import java.sql.SQLException
import java.sql.SQLIntegrityConstraintViolationException
import java.util.concurrent.Callable

/**
 * Interface for a verticle that exposes a REST API for users
 */
interface UserAPIVerticle : Verticle

/**
 * Verticle that exposes a REST API for users
 * @param service the user service
 */
open class RESTUserAPIVerticle(private val service: UserService) : AbstractVerticle(), UserAPIVerticle {
    private val logger: Logger = LogManager.getLogger(this::class)

    /**
     * Companion object to hold utility methods
     */
    companion object {
        private val logger: Logger = LogManager.getLogger(this::class)

        /**
         * Send a response with a status code
         * @param ctx the routing context
         * @param statusCode the status code
         */
        protected fun sendResponse(ctx: RoutingContext, statusCode: Int) {
            logger.trace("Sending response with status code: {}", statusCode)
            ctx.response()
                .setStatusCode(statusCode)
                .end()
        }

        /**
         * Send a response with a status code and a message
         * @param ctx the routing context
         * @param statusCode the status code
         * @param message the message
         */
        @JvmStatic
        protected fun sendResponse(ctx: RoutingContext, statusCode: Int, message: String?) {
            logger.trace("Sending response with status code: {} and message: {}", statusCode, message)
            ctx.response()
                .setStatusCode(statusCode)
                .end(message)
        }

        /**
         * Send an error response
         * @param ctx the routing context
         * @param error the error
         */
        @JvmStatic
        protected fun sendErrorResponse(ctx: RoutingContext, error: Throwable) {
            when (error) {
                is IllegalArgumentException -> sendResponse(ctx, StatusCode.BAD_REQUEST, error.message)
                is IllegalStateException -> sendResponse(ctx, StatusCode.NOT_FOUND, error.message)
                is SQLIntegrityConstraintViolationException -> sendResponse(ctx, StatusCode.FORBIDDEN, error.message)
                is SQLException -> sendResponse(ctx, StatusCode.INTERNAL_SERVER_ERROR, error.message)
                else -> sendResponse(ctx, StatusCode.INTERNAL_SERVER_ERROR, error.message)
            }
        }
    }

    /**
     * Start the verticle and expose the REST API.
     */
    override fun start() {
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())

        addEndPoints(router)

        this.vertx.createHttpServer(options()).requestHandler(router).listen(Port.HTTP)
    }

    protected open fun addEndPoints(router: Router) {
        router.get(Endpoint.HEALTH).handler { ctx ->
            ctx.response().end("OK")
        }

        router.post(Endpoint.USER).handler(::addUser)
        router.get(Endpoint.USER).handler(::getUser)
        router.put(Endpoint.USER).handler(::updateUser)
    }

    protected open fun options() = HttpServerOptions()

    /**
     * Handler to add a user
     * @param ctx the routing context
     */
    private fun addUser(ctx: RoutingContext) {
        vertx.executeBlocking(
            Callable {
                val requestBody = ctx.body().asJsonObject()
                logger.debug("Received POST request with body: '{}'", requestBody)

                val email = requestBody.getString("email") ?: throw IllegalArgumentException("Email is required")
                val username = requestBody.getString("username") ?: throw IllegalArgumentException("Username is required")

                logger.trace("about to add user")
                service.addUser(User.of(email, username))
            }
        ).onComplete {
            if (it.succeeded()) {
                logger.trace("user added successfully")
                sendResponse(ctx, StatusCode.CREATED)
            } else {
                logger.warn("failed to add user:", it.cause())
                sendErrorResponse(ctx, it.cause())
            }
        }
    }

    /**
     * Handler to retrieve a user
     * @param ctx the routing context
     */
    private fun getUser(ctx: RoutingContext) {
        vertx.executeBlocking(
            Callable {
                val email = ctx.request().getParam("email") ?: throw IllegalArgumentException("email is required")
                logger.debug("Received GET request with id: '{}'", email)

                val user = service.getUser(User.userIDOf(email)) ?: throw IllegalStateException("User not found")
                logger.trace("user found: '{}','{}'", user.email, user.username)

                val jsonUser = JsonObject()
                    .put("email", user.email)
                    .put("username", user.username)
                jsonUser
            }
        ).onComplete {
            if (it.succeeded()) {
                logger.trace("user found successfully")
                sendResponse(ctx, StatusCode.OK, it.result().toString())
            } else {
                logger.warn("failed to find user:", it.cause())
                sendErrorResponse(ctx, it.cause())
            }
        }
    }

    /**
     * Handler to update a user
     * @param ctx the routing context
     */
    private fun updateUser(ctx: RoutingContext) {
        vertx.executeBlocking(
            Callable {
                val requestBody = ctx.body().asJsonObject()
                logger.debug("Received PUT request with body: '{}'", requestBody)

                val email = requestBody.getString("email") ?: throw IllegalArgumentException("Email is required")
                val newUsername = requestBody.getString("username") ?: throw IllegalArgumentException("Username is required")

                logger.trace("about to update user")
                service.updateUser(User.of(email, newUsername))
            }
        ).onComplete {
            if (it.succeeded()) {
                logger.trace("user updated successfully")
                sendResponse(ctx, StatusCode.OK)
            } else {
                logger.warn("failed to update user:", it.cause())
                sendErrorResponse(ctx, it.cause())
            }
        }
    }
}
