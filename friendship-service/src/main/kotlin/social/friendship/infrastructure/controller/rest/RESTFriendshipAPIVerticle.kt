package social.friendship.infrastructure.controller.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vertx.core.AbstractVerticle
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
import java.sql.SQLException
import java.sql.SQLIntegrityConstraintViolationException
import java.util.concurrent.Callable
import kotlin.String

class RESTFriendshipAPIVerticle(private val service: FriendshipService) : AbstractVerticle() {
    private val logger: Logger = LogManager.getLogger(this::class)
    private val mapper: ObjectMapper = jacksonObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
    }

    companion object {
        private val logger: Logger = LogManager.getLogger(this::class)

        private fun sendResponse(ctx: RoutingContext, statusCode: Int) {
            logger.trace("Sending response with status code: {}", statusCode)
            ctx.response()
                .setStatusCode(statusCode)
                .end()
        }

        private fun sendResponse(ctx: RoutingContext, statusCode: Int, message: String?) {
            logger.trace("Sending response with status code: {} and message: {}", statusCode, message)
            ctx.response()
                .setStatusCode(statusCode)
                .end(message)
        }

        private fun sendErrorResponse(ctx: RoutingContext, error: Throwable) {
            when (error) {
                is IllegalArgumentException, is MismatchedInputException -> sendResponse(ctx, StatusCode.BAD_REQUEST, error.message)
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

    override fun start() {
        createHttpServer()
    }

    private fun createHttpServer() {
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())

        router.post(Endpoint.FRIENDSHIP).handler(::addFriendship)
        router.get(Endpoint.FRIENDSHIP).handler(::getFriendship)

        router.get(Endpoint.FRIENDSHIP_REQUEST).handler(::getFriendshipRequest)
        router.post(Endpoint.FRIENDSHIP_REQUEST_SEND).handler(::addFriendshipRequest)
        router.put(Endpoint.FRIENDSHIP_REQUEST_ACCEPT).handler(::acceptFriendshipRequest)
        router.put(Endpoint.FRIENDSHIP_REQUEST_DECLINE).handler(::rejectFriendshipRequest)

        router.post(Endpoint.MESSAGE_SEND).handler(::addMessage)
        router.get(Endpoint.MESSAGE_RECEIVE).handler(::getMessagesReceived)
        router.get(Endpoint.MESSAGE_CHAT).handler(::getChat)

        this.vertx.createHttpServer()
            .requestHandler(router)
            .listen(Port.HTTP)
    }

    private fun addFriendship(ctx: RoutingContext) {
        vertx.executeBlocking(
            Callable {
                val requestBody = ctx.body().asString()
                logger.debug("Received POST request with body: '{}'", requestBody)

                val friendship: Friendship = mapper.readValue(requestBody, Friendship::class.java)
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

    private fun getFriendship(ctx: RoutingContext) {
        vertx.executeBlocking(
            Callable {
                if (ctx.request().params().isEmpty) {
                    throw IllegalArgumentException("cannot execute get request without parameters")
                } else {
                    val userId = ctx.request().getParam("id") ?: throw IllegalArgumentException("user 'id' is required")
                    logger.debug("Received GET request with 'id': {}", userId)

                    val user = User.of(userId)
                    val usersRetrieved = service.getAllFriendsByUserId(user.id)

                    logger.trace("users retrieved: '{}'", usersRetrieved)
                    mapper.writeValueAsString(usersRetrieved)
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

    private fun addFriendshipRequest(ctx: RoutingContext) {
        vertx.executeBlocking(
            Callable {
                val requestBody = ctx.body().asString()
                logger.debug("Received POST request with body: '{}'", requestBody)

                val friendshipRequest: FriendshipRequest = mapper.readValue(requestBody, FriendshipRequest::class.java)
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

    private fun acceptFriendshipRequest(ctx: RoutingContext) {
        vertx.executeBlocking(
            Callable {
                val requestBody = ctx.body().asString()
                logger.debug("Received PUT request with body: '{}'", requestBody)

                val friendshipRequest: FriendshipRequest = mapper.readValue(requestBody, FriendshipRequest::class.java)
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

    private fun getFriendshipRequest(ctx: RoutingContext) {
        vertx.executeBlocking(
            Callable {
                if (ctx.request().params().isEmpty) {
                    throw IllegalArgumentException("cannot execute get request without parameters")
                } else {
                    val userId = ctx.request().getParam("id") ?: throw IllegalArgumentException("user 'id' is required")
                    logger.debug("Received GET request: 'id': '{}'", userId)

                    val user = User.of(userId)
                    val friendshipRequestsRetrieved = service.getAllFriendshipRequestsByUserId(user.id)

                    logger.trace("friendship request retrieved: '{}'", friendshipRequestsRetrieved)
                    mapper.writeValueAsString(friendshipRequestsRetrieved)
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

    private fun addMessage(ctx: RoutingContext) {
        vertx.executeBlocking(
            Callable {
                val requestBody = ctx.body().asString()
                logger.debug("Received POST request: '{}'", requestBody)

                val message: Message = mapper.readValue(requestBody, Message::class.java)
                service.addMessage(message)
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

    private fun getMessagesReceived(ctx: RoutingContext) {
        vertx.executeBlocking(
            Callable {
                if (ctx.request().params().isEmpty) {
                    throw IllegalArgumentException("cannot execute get request without parameters")
                } else {
                    val userId = ctx.request().getParam("id") ?: throw IllegalArgumentException("user 'id' is required")
                    logger.debug("Received GET request: 'id': '{}'", userId)

                    val user = User.of(userId)
                    val messagesRetrieved = service.getAllMessagesReceivedByUserId(user.id)

                    logger.trace("message retrieved: '{}'", messagesRetrieved)
                    mapper.writeValueAsString(messagesRetrieved)
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

    private fun getChat(ctx: RoutingContext) {
        vertx.executeBlocking(
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
                    mapper.writeValueAsString(messagesRetrieved)
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
}
