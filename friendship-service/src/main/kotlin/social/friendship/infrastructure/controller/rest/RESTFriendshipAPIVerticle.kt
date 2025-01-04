package social.friendship.infrastructure.controller.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kafka.client.producer.KafkaProducer
import io.vertx.kafka.client.producer.KafkaProducerRecord
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import social.common.endpoint.Endpoint
import social.common.endpoint.Port
import social.common.endpoint.StatusCode
import social.common.events.FriendshipRequestAccepted
import social.friendship.domain.Friendship
import social.friendship.domain.Friendship.FriendshipID
import social.friendship.domain.FriendshipRequest
import social.friendship.domain.FriendshipRequest.FriendshipRequestID
import social.friendship.domain.Message
import social.friendship.domain.Message.MessageID
import social.friendship.infrastructure.controller.event.KafkaFriendshipProducer
import social.friendship.social.friendship.domain.User
import social.friendship.social.friendship.domain.application.FriendshipService
import social.friendship.social.friendship.domain.application.FriendshipServiceImpl
import social.friendship.social.friendship.infrastructure.persistence.sql.DatabaseCredentials
import social.friendship.social.friendship.infrastructure.persistence.sql.FriendshipRequestSQLRepository
import social.friendship.social.friendship.infrastructure.persistence.sql.FriendshipSQLRepository
import social.friendship.social.friendship.infrastructure.persistence.sql.MessageSQLRepository
import social.friendship.social.friendship.infrastructure.persistence.sql.UserSQLRepository
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.SQLException
import java.sql.SQLIntegrityConstraintViolationException
import java.util.concurrent.Callable
import kotlin.String

class RESTFriendshipAPIVerticle(val credentials: DatabaseCredentials? = null) : AbstractVerticle() {
    private val logger: Logger = LogManager.getLogger(this::class)
    private val userSQLRepository = UserSQLRepository()
    private val friendshipRepository = FriendshipSQLRepository()
    private val friendshipRequestRepository = FriendshipRequestSQLRepository()
    private val messageRepository = MessageSQLRepository()
    private val friendshipService: FriendshipService<FriendshipID, Friendship> = FriendshipServiceImpl(friendshipRepository)
    private val friendshipRequestService: FriendshipService<FriendshipRequestID, FriendshipRequest> = FriendshipServiceImpl(friendshipRequestRepository)
    private val messageService: FriendshipService<MessageID, Message> = FriendshipServiceImpl(messageRepository)
    private lateinit var producer: KafkaProducer<String, String>
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
                is SQLException -> sendResponse(ctx, StatusCode.INTERNAL_SERVER_ERROR, error.message)
                else -> sendResponse(ctx, StatusCode.INTERNAL_SERVER_ERROR, error.message)
            }
        }
    }

    override fun start() {
        if (credentials != null) {
            connectToDatabase(credentials)
        } else {
            connectToDefaultDatabase()
        }
        initializeKafkaEventProducer()
        createHttpServer()
    }

    private fun connectToDatabase(credentials: DatabaseCredentials) {
        listOf(userSQLRepository, friendshipRepository, friendshipRequestRepository, messageRepository).forEach {
            it.connect(credentials.host, credentials.port, credentials.dbName, credentials.username, credentials.password)
        }
    }

    private fun connectToDefaultDatabase() {
        val host = System.getenv("DB_HOST")
        val port = System.getenv("DB_PORT")
        val dbName = System.getenv("MYSQL_DATABASE")
        val username = System.getenv("MYSQL_USER")
        val password = Files.readString(Paths.get("/run/secrets/db_password")).trim()

        connectToDatabase(DatabaseCredentials(host, port, dbName, username, password))
    }

    private fun initializeKafkaEventProducer() {
        producer = KafkaFriendshipProducer.createProducer(vertx)
    }

    private fun createHttpServer() {
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())

        router.post(Endpoint.FRIENDSHIP).handler(::addFriendship)
        router.get(Endpoint.FRIENDSHIP).handler(::getFriendship)
        router.post(Endpoint.FRIENDSHIP_REQUEST).handler(::addFriendshipRequest)
        router.get(Endpoint.FRIENDSHIP_REQUEST).handler(::getFriendshipRequest)
        router.post(Endpoint.MESSAGE).handler(::addMessage)
        router.get(Endpoint.MESSAGE).handler(::getMessage)

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
                friendshipService.add(friendship)

                val friendshipJsonString = mapper.writeValueAsString(friendship)
                producer.write(KafkaProducerRecord.create(FriendshipRequestAccepted.TOPIC, friendshipJsonString))
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
                val requestedUserToID = ctx.request().getParam("to") ?: throw IllegalArgumentException("friendship 'to' is required")
                val requestedUserFromID = ctx.request().getParam("from") ?: throw IllegalArgumentException("friendship 'from' is required")
                logger.debug("Received GET request with 'to': '{}' and 'from': '{}'", requestedUserToID, requestedUserFromID)

                val userTo = User.of(requestedUserToID)
                val userFrom = User.of(requestedUserFromID)
                val friendshipToCheckExistenceOf = Friendship.of(userTo, userFrom)

                val friendshipRetrieved = friendshipService.getById(friendshipToCheckExistenceOf.id) ?: throw IllegalStateException("friendship not found")
                logger.trace("friendship retrieved: '{}'", friendshipRetrieved)

                mapper.writeValueAsString(friendshipRetrieved)
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
                friendshipRequestService.add(friendshipRequest)

                val friendshipRequestJsonString = mapper.writeValueAsString(friendshipRequest)
                producer.write(KafkaProducerRecord.create(FriendshipRequestAccepted.TOPIC, friendshipRequestJsonString))
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

    private fun getFriendshipRequest(ctx: RoutingContext) {
        vertx.executeBlocking(
            Callable {
                val requestedUserToID = ctx.request().getParam("to") ?: throw IllegalArgumentException("friendship request 'to' is required")
                val requestedUserFromID = ctx.request().getParam("from") ?: throw IllegalArgumentException("friendship request 'from' is required")
                logger.debug("Received GET request: 'to': '{}' and 'from': '{}'", requestedUserToID, requestedUserFromID)

                val userTo = User.of(requestedUserToID)
                val userFrom = User.of(requestedUserFromID)
                val friendshipRequestToCheckExistenceOf = FriendshipRequest.of(userTo, userFrom)

                val friendshipRequestRetrieved = friendshipRequestService.getById(friendshipRequestToCheckExistenceOf.id) ?: throw IllegalStateException("friendship request not found")
                logger.trace("friendship request retrieved: '{}'", friendshipRequestRetrieved)

                mapper.writeValueAsString(friendshipRequestRetrieved)
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
                messageService.add(message)

                val messageJsonString = mapper.writeValueAsString(message)
                producer.write(KafkaProducerRecord.create(FriendshipRequestAccepted.TOPIC, messageJsonString))
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

    private fun getMessage(ctx: RoutingContext) {
        vertx.executeBlocking(
            Callable {
                val requestedUserToID = ctx.request().getParam("to") ?: throw IllegalArgumentException("message 'to' is required, because messages can be exchanged only if a friendship is present")
                val requestedUserFromID = ctx.request().getParam("from") ?: throw IllegalArgumentException("message 'from' is required, because messages can be exchanged only if a friendship is present")
                val requestedMessageID = ctx.request().getParam("id") ?: throw IllegalArgumentException("message 'id' is required")
                logger.debug("Received GET request: 'to': '{}', 'from': '{}', 'id': '{}'", requestedUserToID, requestedUserFromID, requestedMessageID)

                val userTo = User.of(requestedUserToID)
                val userFrom = User.of(requestedUserFromID)
                val friendship = Friendship.of(userTo, userFrom)
                val messageToCheckExistenceOf = Message.of(friendship, requestedMessageID)

                val messageRetrieved = messageService.getById(messageToCheckExistenceOf.id) ?: throw IllegalStateException("message not found")
                logger.trace("message retrieved: '{}'", messageRetrieved)

                mapper.writeValueAsString(messageRetrieved)
            }
        ).onComplete {
            if (it.succeeded()) {
                logger.trace("message retrieved successfully")
                sendResponse(ctx, StatusCode.OK, it.result().toString())
            } else {
                logger.warn("failed to get message:", it.cause())
                sendErrorResponse(ctx, it.cause())
            }
        }
    }
}
