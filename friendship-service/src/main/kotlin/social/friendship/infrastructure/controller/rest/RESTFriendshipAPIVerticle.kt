package social.friendship.infrastructure.controller.rest

import com.fasterxml.jackson.databind.ObjectMapper
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
import social.friendship.infrastructure.controller.event.KafkaFriendshipProducer
import social.friendship.social.friendship.domain.User
import social.friendship.social.friendship.domain.application.FriendshipService
import social.friendship.social.friendship.domain.application.FriendshipServiceImpl
import social.friendship.social.friendship.infrastructure.persistence.sql.DatabaseCredentials
import social.friendship.social.friendship.infrastructure.persistence.sql.FriendshipRequestSQLRepository
import social.friendship.social.friendship.infrastructure.persistence.sql.FriendshipSQLRepository
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
    private val friendshipService: FriendshipService<FriendshipID, Friendship> = FriendshipServiceImpl(friendshipRepository)
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
                is IllegalArgumentException -> sendResponse(ctx, StatusCode.BAD_REQUEST, error.message)
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
        listOf(userSQLRepository, friendshipRepository, friendshipRequestRepository).forEach {
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

        this.vertx.createHttpServer()
            .requestHandler(router)
            .listen(Port.HTTP)
    }

    private fun addFriendship(ctx: RoutingContext) {
        vertx.executeBlocking(
            Callable {
                val requestBody = ctx.body().asJsonObject()
                logger.debug("Received POST request with body: '{}'", requestBody)

                val requestedUserToID: String = requestBody.getString("to") ?: throw IllegalArgumentException("friendship 'to' is required")
                val requestedUserFromID: String = requestBody.getString("from") ?: throw IllegalArgumentException("friendship 'from' is required")

                val userTo = User.of(requestedUserToID)
                val userFrom = User.of(requestedUserFromID)

                logger.trace("about to add friendship")
                val friendship = Friendship.of(userTo, userFrom)
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
}
