package social.user.infrastructure.controller.rest

import com.fasterxml.jackson.core.JsonParseException
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.http.HttpServerOptions
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
import social.user.domain.UserID
import social.user.infrastructure.serialization.jackson.Mapper
import java.sql.SQLException
import java.sql.SQLIntegrityConstraintViolationException
import java.util.concurrent.Callable

/**
 * Verticle that exposes a REST API for users
 * @param service the user service
 */
open class UserApiVerticle(protected val service: UserService) : AbstractVerticle() {
    private val logger: Logger = LogManager.getLogger(this::class)

    /**
     * Send a response with a status code and a message
     * @param ctx the routing context
     * @param statusCode the status code
     * @param msg the message
     */
    private fun sendResponse(ctx: RoutingContext, statusCode: Int, msg: String? = null) {
        logger.trace("Sending response with status code: {} and message: {}", statusCode, msg)
        ctx.response()
            .setStatusCode(statusCode)
            .apply { if (msg == null) end() else end(msg) }
    }

    /**
     * Send an error response
     * @param ctx the routing context
     * @param error the error
     */
    private fun sendErrorResponse(ctx: RoutingContext, error: Throwable) {
        when (error) {
            is IllegalArgumentException, is JsonParseException -> sendResponse(ctx, StatusCode.BAD_REQUEST, error.message)
            is IllegalStateException -> sendResponse(ctx, StatusCode.NOT_FOUND, error.message)
            is SQLIntegrityConstraintViolationException -> sendResponse(ctx, StatusCode.FORBIDDEN, error.message)
            is SQLException -> sendResponse(ctx, StatusCode.INTERNAL_SERVER_ERROR, error.message)
            else -> sendResponse(ctx, StatusCode.INTERNAL_SERVER_ERROR, error.message)
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
            sendResponse(ctx, StatusCode.OK)
        }

        router.get(Endpoint.USER_COUNT).handler(::getUserCount)
        router.post(Endpoint.USER).handler(::addUser)
        router.get(Endpoint.USER).handler(::getUser)
        router.delete(Endpoint.USER_EMAIL_PARAM).handler(::deleteUser)
        router.post(Endpoint.BLOCK_USER).handler(::blockUser)
        router.post(Endpoint.UNBLOCK_USER).handler(::unblockUser)
    }

    protected open fun options() = HttpServerOptions()

    /**
     * Handler to add a user
     * @param ctx the routing context
     */
    private fun addUser(ctx: RoutingContext) {
        this.context.executeBlocking(
            Callable {
                val requestBody = ctx.body().asString()
                    ?: throw IllegalArgumentException("Request body is missing")
                logger.debug("Received POST request with body: '{}'", requestBody)
                val user = Mapper.readValue(requestBody, User::class.java)
                logger.trace("about to add user")
                service.addUser(user)
            }
        ).onComplete(respond(ctx, StatusCode.CREATED))
    }

    /**
     * Handler to retrieve a user
     * @param ctx the routing context
     */
    private fun getUser(ctx: RoutingContext) {
        this.context.executeBlocking(
            Callable {
                val email = ctx.request().getParam("email")
                    ?: throw IllegalArgumentException("email is required")
                logger.debug("Received GET request with id: '{}'", email)
                val user = service.getUser(UserID.of(email))
                    ?: throw IllegalStateException("User not found")
                logger.trace("user found: '{}','{}'", user.email, user.username)
                Mapper.writeValueAsString(user)
            }
        ).onComplete(respond(ctx, StatusCode.OK))
    }

    private fun blockUser(ctx: RoutingContext) {
        this.context.executeBlocking(
            Callable {
                val email = UserID.of(ctx.pathParam(Endpoint.EMAIL_PARAM))
                service.blockUser(email)
            }
        ).onComplete(respond(ctx, StatusCode.NO_CONTENT))
    }

    private fun unblockUser(ctx: RoutingContext) {
        this.context.executeBlocking(
            Callable {
                val email = UserID.of(ctx.pathParam(Endpoint.EMAIL_PARAM))
                service.unblockUser(email)
            }
        ).onComplete(respond(ctx, StatusCode.NO_CONTENT))
    }

    private fun deleteUser(context: RoutingContext) {
        this.context.executeBlocking(
            Callable {
                val email = context.pathParam(Endpoint.EMAIL_PARAM)
                service.deleteUser(UserID.of(email))
                    ?: throw IllegalStateException("User not found")
            }
        ).onComplete(respond(context, StatusCode.NO_CONTENT))
    }

    private fun getUserCount(ctx: RoutingContext) {
        this.context.executeBlocking(
            Callable {
                service.getUserCount()
            }
        ).onComplete(respond(ctx, StatusCode.OK))
    }

    protected fun <T> respond(ctx: RoutingContext, statusCode: Int): (AsyncResult<T>) -> Unit =
        {
            if (it.succeeded())
                sendResponse(ctx, statusCode, it.result()?.toString())
            else
                sendErrorResponse(ctx, it.cause())
        }
}
