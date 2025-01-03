package social.friendship.infrastructure.controller.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.codec.BodyCodec
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import social.common.endpoint.Endpoint
import social.common.endpoint.StatusCode
import social.friendship.domain.Friendship
import social.friendship.infrastructure.DockerSQLTest
import social.friendship.social.friendship.domain.User
import social.friendship.social.friendship.infrastructure.persistence.sql.DatabaseCredentials
import java.io.File
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals

object RESTFriendshipAPIVerticleTest : DockerSQLTest() {
    private val logger = LogManager.getLogger(this::class)
    private val user1 = User.of("user1ID")
    private val user2 = User.of("user2ID")
    private val friendship = Friendship.of(user1, user2)
    private lateinit var webClient: WebClient
    private lateinit var dockerComposeFile: File
    private val api = RESTFriendshipAPIVerticle(DatabaseCredentials(host, port, database, user, password))
    private val mapper: ObjectMapper = jacksonObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
    }

    @JvmStatic
    @BeforeAll
    fun setUpAll() {
        dockerComposeFile = generateDockerComposeFile()
    }

    @BeforeEach
    fun setUp() {
        executeDockerComposeCmd(dockerComposeFile, "up", "--wait")

        val vertx = Vertx.vertx()
        startRESTFriendshipAPIVerticle(vertx)
        createTestWebClient(vertx)
    }

    private fun startRESTFriendshipAPIVerticle(vertx: Vertx) {
        val latch = CountDownLatch(1)
        vertx.deployVerticle(api).onComplete {
            latch.countDown()
            if (it.succeeded()) {
                logger.info("Friendship REST verticle started")
            } else {
                logger.error("Failed to start friendship API:", it.cause())
            }
        }
        latch.await()
    }

    private fun createTestWebClient(vertx: Vertx) {
        webClient = WebClient.create(vertx, WebClientOptions().setDefaultPort(8080).setDefaultHost("localhost"))
    }

    @AfterEach
    fun tearDown() {
        executeDockerComposeCmd(dockerComposeFile, "down", "-v")
    }

    private fun sendPostRequest(
        send: JsonObject?,
        latch: CountDownLatch,
        endpoint: String
    ): HttpResponse<String> {
        val responseLatch = CountDownLatch(1)
        lateinit var response: HttpResponse<String>
        webClient.post(endpoint)
            .putHeader("content-type", "application/json")
            .`as`(BodyCodec.string())
            .sendJsonObject(send) { ar ->
                latch.countDown()
                if (ar.succeeded()) {
                    response = ar.result()
                } else {
                    throw ar.cause()
                }
                responseLatch.countDown()
            }
        responseLatch.await()
        return response
    }

    @Timeout(60)
    @Test
    fun addFriendshipWithoutUsersParam() {
        val latch = CountDownLatch(2)

        val friendshipJsonString = mapper.writeValueAsString(friendship)
        val friendshipJson = JsonObject(friendshipJsonString)

        val friendshipWithoutUserToJson = friendshipJson.copy()
        friendshipWithoutUserToJson.remove("to")
        val response1 = sendPostRequest(friendshipWithoutUserToJson, latch, Endpoint.FRIENDSHIP)

        val friendshipWithoutUserFromJson = friendshipJson.copy()
        friendshipWithoutUserFromJson.remove("from")
        val response2 = sendPostRequest(friendshipWithoutUserFromJson, latch, Endpoint.FRIENDSHIP)

        latch.await()
        assertAll(
            { assertEquals(StatusCode.BAD_REQUEST, response1.statusCode()) },
            { assertEquals(StatusCode.BAD_REQUEST, response2.statusCode()) }
        )
    }

    @Timeout(60)
    @Test
    fun addFriendshipWithoutFriendshipRequestAndUsers() {
        val latch = CountDownLatch(1)

        val friendshipJsonString = mapper.writeValueAsString(friendship)
        val friendshipJson = JsonObject(friendshipJsonString)

        val response = sendPostRequest(friendshipJson, latch, Endpoint.FRIENDSHIP)

        latch.await()
        assertEquals(StatusCode.FORBIDDEN, response.statusCode())
    }

    @Timeout(60)
    @Test
    fun addFriendshipRequestWithoutUsers() {
        val latch = CountDownLatch(1)

        val friendshipJsonString = mapper.writeValueAsString(friendship)
        val friendshipJson = JsonObject(friendshipJsonString)

        val response = sendPostRequest(friendshipJson, latch, Endpoint.FRIENDSHIP_REQUEST)

        latch.await()
        assertEquals(StatusCode.FORBIDDEN, response.statusCode())
    }

    private fun sendGetRequest(paramName: String, paramValue: String, latch: CountDownLatch, endpoint: String): HttpResponse<String> {
        val responseLatch = CountDownLatch(1)
        lateinit var response: HttpResponse<String>
        webClient.get(endpoint)
            .addQueryParam(paramName, paramValue)
            .putHeader("content-type", "application/json")
            .`as`(BodyCodec.string())
            .send { ar ->
                latch.countDown()
                if (ar.succeeded()) {
                    response = ar.result()
                } else {
                    throw ar.cause()
                }
                responseLatch.countDown()
            }
        responseLatch.await()
        return response
    }

    @Timeout(60)
    @Test
    fun getFriendshipWithoutUsersParam() {
        val latch = CountDownLatch(2)

        val response1 = sendGetRequest("to", friendship.to.id.value, latch, Endpoint.FRIENDSHIP)
        val response2 = sendGetRequest("from", friendship.from.id.value, latch, Endpoint.FRIENDSHIP)

        latch.await()
        assertAll(
            { assertEquals(StatusCode.BAD_REQUEST, response1.statusCode()) },
            { assertEquals(StatusCode.BAD_REQUEST, response2.statusCode()) }
        )
    }

    @Timeout(60)
    @Test
    fun getFriendshipRequestWithoutUsersParam() {
        val latch = CountDownLatch(1)

        val response1 = sendGetRequest("to", friendshipRequest.to.id.value, latch, Endpoint.FRIENDSHIP_REQUEST)
        val response2 = sendGetRequest("from", friendshipRequest.from.id.value, latch, Endpoint.FRIENDSHIP_REQUEST)

        latch.await()
        assertAll(
            { assertEquals(StatusCode.BAD_REQUEST, response1.statusCode()) },
            { assertEquals(StatusCode.BAD_REQUEST, response2.statusCode()) }
        )
    }
}
