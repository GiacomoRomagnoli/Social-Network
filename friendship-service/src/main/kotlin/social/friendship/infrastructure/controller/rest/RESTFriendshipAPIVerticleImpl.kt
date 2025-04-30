package social.friendship.infrastructure.controller.rest

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import io.vertx.core.AbstractVerticle
import io.vertx.core.Verticle
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import social.common.endpoint.Endpoint
import social.common.endpoint.Port
import social.common.endpoint.StatusCode
import social.friendship.application.FriendshipService
import social.friendship.domain.Friendship
import social.friendship.domain.FriendshipRequest
import social.friendship.domain.Message
import social.friendship.domain.User
import social.friendship.infrastructure.persistence.sql.SQLStateError
import social.friendship.social.friendship.infrastructure.serialization.jackson.Mapper
import java.sql.SQLException
import java.sql.SQLIntegrityConstraintViolationException
import java.util.UUID
import java.util.concurrent.Callable
import kotlin.String

/**
 * Verticle that exposes the REST API for the friendship service.
 */
interface RESTFriendshipAPIVerticle : Verticle

/**
 * Verticle that exposes the REST API for the friendship service.
 * @param service the friendship service
 */
class RESTFriendshipAPIVerticleImpl(private val service: FriendshipService) : AbstractVerticle(), RESTFriendshipAPIVerticle {
    private val logger: Logger = LogManager.getLogger(this::class)

    /**
     * Companion object that contains utility methods for the REST API.
     */
    companion object {
        private val logger: Logger = LogManager.getLogger(this::class)

        /**
         * Send a response with a status code.
         * @param ctx the routing context
         * @param statusCode the status code
         */
        private fun sendResponse(ctx: RoutingContext, statusCode: Int) {
            logger.trace("Sending response with status code: {}", statusCode)
            ctx.response()
                .setStatusCode(statusCode)
                .end()
        }

        /**
         * Send a response with a status code and a message.
         * @param ctx the routing context
         * @param statusCode the status code
         * @param message the message
         */
        private fun sendResponse(ctx: RoutingContext, statusCode: Int, message: String?) {
            logger.trace("Sending response with status code: {} and message: {}", statusCode, message)
            ctx.response()
                .setStatusCode(statusCode)
                .end(message)
        }

        /**
         * Send an error response.
         * @param ctx the routing context
         * @param error the error
         */
        private fun sendErrorResponse(ctx: RoutingContext, error: Throwable) {
            when (error) {
                is IllegalArgumentException, is MismatchedInputException, is JsonParseException -> sendResponse(
                    ctx,
                    StatusCode.BAD_REQUEST,
                    error.message
                )
                is IllegalStateException -> sendResponse(ctx, StatusCode.NOT_FOUND, error.message)
                is SQLIntegrityConstraintViolationException -> sendResponse(ctx, StatusCode.FORBIDDEN, error.message)
                is SQLException -> {
                    when (error.sqlState) {
                        SQLStateError.MISSING_FRIENDSHIP -> sendResponse(ctx, StatusCode.FORBIDDEN, error.message)
                        else -> {
                            sendResponse(ctx, StatusCode.INTERNAL_SERVER_ERROR, error.message)
                        }
                    }
                }

                else -> sendResponse(ctx, StatusCode.INTERNAL_SERVER_ERROR, error.message)
            }
        }
    }
    /**
     * Start the verticle. Create the HTTP server and set the routes.
     */
    override fun start() {
        createHttpServer()
    }

    /**
     * Create the HTTP server and set the routes.
     */
    private fun createHttpServer() {
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())

        router.get(Endpoint.HEALTH).handler { ctx ->
            ctx.response().end("OK")
        }

        router.post(Endpoint.FRIENDSHIP).handler(::addFriendship)
        router.get(Endpoint.FRIENDSHIP).handler(::getFriendship)

        router.get(Endpoint.FRIENDSHIP_REQUEST).handler(::getFriendshipRequest)
        router.post(Endpoint.FRIENDSHIP_REQUEST_SEND).handler(::addFriendshipRequest)
        router.put(Endpoint.FRIENDSHIP_REQUEST_ACCEPT).handler(::acceptFriendshipRequest)
        router.put(Endpoint.FRIENDSHIP_REQUEST_DECLINE).handler(::rejectFriendshipRequest)

        router.post(Endpoint.MESSAGE_SEND).handler(::addMessage)
        router.get(Endpoint.MESSAGE_RECEIVE).handler(::getMessagesReceived)
        router.get(Endpoint.MESSAGE_CHAT).handler(::getChat)
        router.get(Endpoint.MESSAGE).handler(::getMessage)

        this.vertx.createHttpServer()
            .requestHandler(router)
            .listen(Port.HTTP)
    }

    /**
     * Handler to add a friendship.
     * @param ctx the routing context
     */
    private fun addFriendship(ctx: RoutingContext) {
        this.context.executeBlocking(
            Callable {
                val requestBody = ctx.body()
                logger.debug("Received POST request with body: '{}'", requestBody)

                val friendship = Mapper.readValue(requestBody.asString(), Friendship::class.java)

                service.addFriendship(friendship)
            }
        ).onComplete {
            if (it.succeeded()) {
                logger.trace("friendship added successfully")
                sendResponse(ctx, StatusCode.CREATED)
            } else {
                logger.warn("failed to add friendship:", it.cause())
                sendErrorResponse(ctx, it.cause())
            }
        }
    }

    /**
     * Handler to retrieve a friendship
     * @param ctx the routing context
     */
    private fun getFriendship(ctx: RoutingContext) {
        this.context.executeBlocking(
            Callable {
                if (ctx.request().params().isEmpty) {
                    throw IllegalArgumentException("cannot execute get request without parameters")
                } else {
                    val userId = ctx.request().getParam("id") ?: throw IllegalArgumentException("user 'id' is required")
                    logger.debug("Received GET request with 'id': {}", userId)

                    val user = User.of(userId)
                    val usersRetrieved = service.getAllFriendsByUserId(user.id)

                    logger.trace("users retrieved: '{}'", usersRetrieved)
                    Mapper.writeValueAsString(usersRetrieved)
                }
            }
        ).onComplete {
            if (it.succeeded()) {
                logger.trace("friendship retrieved successfully")
                sendResponse(ctx, StatusCode.OK, it.result().toString())
            } else {
                logger.warn("failed to get friendship:", it.cause())
                sendErrorResponse(ctx, it.cause())
            }
        }
    }

    /**
     * Handler to add a friendship request.
     * @param ctx the routing context
     */
    private fun addFriendshipRequest(ctx: RoutingContext) {
        this.context.executeBlocking(
            Callable {
                val requestBody = ctx.body()
                logger.debug("Received POST request with body: '{}'", requestBody)

                val friendshipRequest = Mapper.readValue(requestBody.asString(), FriendshipRequest::class.java)

                service.addFriendshipRequest(friendshipRequest)
            }
        ).onComplete {
            if (it.succeeded()) {
                logger.trace("friendship request added successfully")
                sendResponse(ctx, StatusCode.CREATED)
            } else {
                logger.warn("failed to add friendship request:", it.cause())
                sendErrorResponse(ctx, it.cause())
            }
        }
    }

    /**
     * Handler to accept a friendship request.
     * @param ctx the routing context
     */
    private fun acceptFriendshipRequest(ctx: RoutingContext) {
        this.context.executeBlocking(
            Callable {
                val requestBody = ctx.body()
                logger.debug("Received PUT request with body: '{}'", requestBody.asString())

                val friendshipRequest = Mapper.readValue(requestBody.asString(), FriendshipRequest::class.java)
                service.acceptFriendshipRequest(friendshipRequest)
            }
        ).onComplete {
            if (it.succeeded()) {
                logger.trace("friendship request updated successfully")
                sendResponse(ctx, StatusCode.OK)
            } else {
                logger.warn("failed to update friendship request:", it.cause())
                sendErrorResponse(ctx, it.cause())
            }
        }
    }

    /**
     * Handler to reject a friendship request.
     * @param ctx the routing context
     */
    private fun rejectFriendshipRequest(ctx: RoutingContext) {
        this.context.executeBlocking(
            Callable {
                val requestBody = ctx.body()
                logger.debug("Received PUT request with body: '{}'", requestBody.asString())

                val friendshipRequest = Mapper.readValue(requestBody.asString(), FriendshipRequest::class.java)
                service.rejectFriendshipRequest(friendshipRequest)
            }
        ).onComplete {
            if (it.succeeded()) {
                logger.trace("friendship request updated successfully")
                sendResponse(ctx, StatusCode.OK)
            } else {
                logger.warn("failed to update friendship request:", it.cause())
                sendErrorResponse(ctx, it.cause())
            }
        }
    }

    /**
     * Handler to retrieve a friendship request.
     * @param ctx the routing context
     */
    private fun getFriendshipRequest(ctx: RoutingContext) {
        this.context.executeBlocking(
            Callable {
                if (ctx.request().params().isEmpty) {
                    throw IllegalArgumentException("cannot execute get request without parameters")
                } else {
                    val userId = ctx.request().getParam("id") ?: throw IllegalArgumentException("user 'id' is required")
                    logger.debug("Received GET request: 'id': '{}'", userId)

                    val user = User.of(userId)
                    val friendshipRequestsRetrieved = service.getAllFriendshipRequestsByUserId(user.id)

                    logger.trace("friendship request retrieved: '{}'", friendshipRequestsRetrieved)
                    Mapper.writeValueAsString(friendshipRequestsRetrieved)
                }
            }
        ).onComplete {
            if (it.succeeded()) {
                logger.trace("friendship request retrieved successfully")
                sendResponse(ctx, StatusCode.OK, it.result().toString())
            } else {
                logger.warn("failed to get friendship request:", it.cause())
                sendErrorResponse(ctx, it.cause())
            }
        }
    }

    /**
     * Handler to add a message.
     * @param ctx the routing context
     */
    private fun addMessage(ctx: RoutingContext) {
        this.context.executeBlocking(
            Callable {
                val requestBody = ctx.body()
                logger.debug("Received POST request: '{}'", requestBody.asString())

                val message = Mapper.readValue(requestBody.asString(), Message::class.java)
                service.sentMessage(message)
            }
        ).onComplete {
            if (it.succeeded()) {
                logger.trace("message added successfully")
                sendResponse(ctx, StatusCode.CREATED)
            } else {
                logger.warn("failed to add message:", it.cause())
                sendErrorResponse(ctx, it.cause())
            }
        }
    }

    /**
     * Handler to retrieve messages received by a user.
     * @param ctx the routing context
     */
    private fun getMessagesReceived(ctx: RoutingContext) {
        this.context.executeBlocking(
            Callable {
                if (ctx.request().params().isEmpty) {
                    throw IllegalArgumentException("cannot execute get request without parameters")
                } else {
                    val userId = ctx.request().getParam("id") ?: throw IllegalArgumentException("user 'id' is required")
                    logger.debug("Received GET request: 'id': '{}'", userId)

                    val user = User.of(userId)
                    val messagesRetrieved = service.getAllMessagesReceivedByUserId(user.id)

                    logger.trace("message retrieved: '{}'", messagesRetrieved)
                    Mapper.writeValueAsString(messagesRetrieved)
                }
            }
        ).onComplete {
            if (it.succeeded()) {
                logger.trace("messages retrieved successfully")
                sendResponse(ctx, StatusCode.OK, it.result().toString())
            } else {
                logger.warn("failed to get messages:", it.cause())
                sendErrorResponse(ctx, it.cause())
            }
        }
    }

    /**
     * Handler to retrieve messages exchanged between two users.
     * @param ctx the routing context
     */
    private fun getChat(ctx: RoutingContext) {
        this.context.executeBlocking(
            Callable {
                if (ctx.request().params().isEmpty) {
                    throw IllegalArgumentException("cannot execute get request without parameters")
                } else {
                    val user1Id = ctx.request().getParam("user1Id") ?: throw IllegalArgumentException("'user1Id' is required")
                    val user2Id = ctx.request().getParam("user2Id") ?: throw IllegalArgumentException("'user2Id' is required")
                    logger.debug("Received GET request: 'user1Id': '{}', 'user2Id': '{}'", user1Id, user2Id)

                    val user1 = User.of(user1Id)
                    val user2 = User.of(user2Id)
                    val messagesRetrieved = service.getAllMessagesExchangedBetween(user1.id, user2.id)

                    logger.trace("message retrieved: '{}'", messagesRetrieved)
                    Mapper.writeValueAsString(messagesRetrieved)
                }
            }
        ).onComplete {
            if (it.succeeded()) {
                logger.trace("messages retrieved successfully")
                sendResponse(ctx, StatusCode.OK, it.result().toString())
            } else {
                logger.warn("failed to get messages:", it.cause())
                sendErrorResponse(ctx, it.cause())
            }
        }
    }

    private fun getMessage(ctx: RoutingContext) {
        this.context.executeBlocking(
            Callable {
                val uuid = ctx.pathParam(Endpoint.UUID)
                val message = service.getMessage(Message.MessageID(UUID.fromString(uuid)))
                    ?: throw IllegalStateException("message not found")
                Mapper.writeValueAsString(message)
            }
        ).onComplete {
            if (it.succeeded()) {
                logger.trace("message retrieved successfully")
                sendResponse(ctx, StatusCode.OK, it.result())
            } else {
                logger.warn("failed to get message:", it.cause())
                sendErrorResponse(ctx, it.cause())
            }
        }
    }
}
