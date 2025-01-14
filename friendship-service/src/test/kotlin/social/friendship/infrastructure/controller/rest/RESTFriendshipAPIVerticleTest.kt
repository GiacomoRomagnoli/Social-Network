package social.friendship.infrastructure.controller.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.codec.BodyCodec
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import social.common.endpoint.Endpoint
import social.common.endpoint.StatusCode
import social.friendship.application.FriendshipServiceVerticle
import social.friendship.domain.Friendship
import social.friendship.domain.FriendshipRequest
import social.friendship.domain.Message
import social.friendship.domain.User
import social.friendship.infrastructure.DockerSQLTest
import social.friendship.infrastructure.persistence.sql.DatabaseCredentials
import java.io.File
import java.util.concurrent.CountDownLatch

object RESTFriendshipAPIVerticleTest : DockerSQLTest() {
    private val logger = LogManager.getLogger(this::class)
    private val user1 = User.of("user1ID")
    private val user2 = User.of("user2ID")
    private val user3 = User.of("user3ID")
    private val user4 = User.of("user4ID")
    private val user5 = User.of("user5ID")
    private val user6 = User.of("user6ID")
    private val friendship1 = Friendship.of(user1, user2)
    private val friendship2 = Friendship.of(user3, user4)
    private val friendship3 = Friendship.of(user5, user6)
    private val friendshipRequest1 = FriendshipRequest.of(user1, user2)
    private val friendshipRequest2 = FriendshipRequest.of(user3, user4)
    private val friendshipRequest3 = FriendshipRequest.of(user5, user6)
    private val message1 = Message.of(user1, user2, "message")
    private val message2 = Message.of(user1, user2, "message2")
    private val message3 = Message.of(user1, user2, "message3")
    private lateinit var webClient: WebClient
    private lateinit var dockerComposeFile: File
    private lateinit var api: RESTFriendshipAPIVerticle
    private lateinit var service: FriendshipServiceVerticle
    private val mapper: ObjectMapper = jacksonObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
    }

    @JvmStatic
    @BeforeAll
    fun setUpAll() {
        dockerComposeFile = generateDockerComposeFile("social/friendship/infrastructure/controller/rest/")
    }

    @BeforeEach
    fun setUp() {
        executeDockerComposeCmd(dockerComposeFile, "up", "--wait")

        val vertx = Vertx.vertx()
        service = FriendshipServiceVerticle(DatabaseCredentials(host, port, database, user, password))
        deployVerticle(vertx, service)
        api = RESTFriendshipAPIVerticle(service)
        deployVerticle(vertx, api)
        createTestWebClient(vertx)
    }

    private fun deployVerticle(vertx: Vertx, verticle: AbstractVerticle) {
        val latch = CountDownLatch(1)
        vertx.deployVerticle(verticle).onComplete {
            latch.countDown()
            if (it.succeeded()) {
                logger.info("Verticle started")
            } else {
                logger.error("Failed to start verticle:", it.cause())
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

    @Timeout(5 * 60)
    @Test
    fun addFriendshipWithoutUsersParam() {
        val latch = CountDownLatch(2)

        val friendshipJsonString = mapper.writeValueAsString(friendship1)
        val friendshipJson = JsonObject(friendshipJsonString)

        val friendshipWithoutUserToJson = friendshipJson.copy()
        friendshipWithoutUserToJson.remove("user1")
        val response1 = sendPostRequest(friendshipWithoutUserToJson, latch, Endpoint.FRIENDSHIP)

        val friendshipWithoutUserFromJson = friendshipJson.copy()
        friendshipWithoutUserFromJson.remove("user2")
        val response2 = sendPostRequest(friendshipWithoutUserFromJson, latch, Endpoint.FRIENDSHIP)

        latch.await()
        assertAll(
            { assertEquals(StatusCode.BAD_REQUEST, response1.statusCode()) },
            { assertEquals(StatusCode.BAD_REQUEST, response2.statusCode()) }
        )
    }

    @Timeout(5 * 60)
    @Test
    fun addFriendshipWithoutFriendshipRequestAndUsers() {
        val latch = CountDownLatch(1)

        val friendshipJsonString = mapper.writeValueAsString(friendship1)
        val friendshipJson = JsonObject(friendshipJsonString)

        val response = sendPostRequest(friendshipJson, latch, Endpoint.FRIENDSHIP)

        latch.await()
        assertEquals(StatusCode.FORBIDDEN, response.statusCode())
    }

    @Timeout(5 * 60)
    @Test
    fun addFriendshipRequestWithoutUsers() {
        val latch = CountDownLatch(1)

        val friendshipJsonString = mapper.writeValueAsString(friendshipRequest1)
        val friendshipJson = JsonObject(friendshipJsonString)

        val response = sendPostRequest(friendshipJson, latch, Endpoint.FRIENDSHIP_REQUEST_SEND)

        latch.await()
        assertEquals(StatusCode.FORBIDDEN, response.statusCode())
    }

    @Timeout(5 * 60)
    @Test
    fun addMessageWithoutFriendship() {
        val latch = CountDownLatch(1)

        val messageJsonString = mapper.writeValueAsString(message1)
        val messageJson = JsonObject(messageJsonString)

        val response = sendPostRequest(messageJson, latch, Endpoint.MESSAGE_SEND)

        latch.await()
        assertEquals(StatusCode.FORBIDDEN, response.statusCode())
    }

    @Timeout(5 * 60)
    @Test
    fun addMessageWhereSenderAndReceiverAreSwapped() {
        val latch = CountDownLatch(1)

        // adds users, friendship request and friendship to the database to be able to add a message
        service.addUser(user1)
        service.addUser(user2)
        service.addFriendshipRequest(friendshipRequest1)
        service.addFriendship(friendship1)

        val messageJsonString = mapper.writeValueAsString(Message.of(user2, user1, "message"))
        val messageJson = JsonObject(messageJsonString)

        val response = sendPostRequest(messageJson, latch, Endpoint.MESSAGE_SEND)

        latch.await()
        assertEquals(StatusCode.CREATED, response.statusCode())
    }

    @Timeout(5 * 60)
    @Test
    fun addFriendshipAboutExistingUsers() {
        val latch = CountDownLatch(1)

        // adds users and friendship request to the database to be able to add a friendship
        service.addUser(user1)
        service.addUser(user2)
        service.addFriendshipRequest(friendshipRequest1)

        val friendshipJsonString = mapper.writeValueAsString(friendship1)
        val friendshipJson = JsonObject(friendshipJsonString)

        val response = sendPostRequest(friendshipJson, latch, Endpoint.FRIENDSHIP)

        latch.await()
        assertEquals(StatusCode.CREATED, response.statusCode())
    }

    @Timeout(5 * 60)
    @Test
    fun addFriendshipRequestAboutExistingUsers() {
        val latch = CountDownLatch(1)

        // adds users to the database to be able to add a friendship request
        service.addUser(user1)
        service.addUser(user2)

        val friendshipRequestJsonString = mapper.writeValueAsString(friendshipRequest1)
        val friendshipRequestJson = JsonObject(friendshipRequestJsonString)

        val response = sendPostRequest(friendshipRequestJson, latch, Endpoint.FRIENDSHIP_REQUEST_SEND)

        latch.await()
        assertEquals(StatusCode.CREATED, response.statusCode())
    }

    @Timeout(5 * 60)
    @Test
    fun addMessageAboutExistingFriendship() {
        val latch = CountDownLatch(1)

        // adds users, friendship request and friendship to the database to be able to add a message
        service.addUser(user1)
        service.addUser(user2)
        service.addFriendshipRequest(friendshipRequest1)
        service.addFriendship(friendship1)

        val messageJsonString = mapper.writeValueAsString(message1)
        val messageJson = JsonObject(messageJsonString)

        val response = sendPostRequest(messageJson, latch, Endpoint.MESSAGE_SEND)

        latch.await()
        assertEquals(StatusCode.CREATED, response.statusCode())
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

    private fun sendGetRequest(paramName: String, paramValue: String, paramName2: String, paramValue2: String, latch: CountDownLatch, endpoint: String): HttpResponse<String> {
        val responseLatch = CountDownLatch(1)
        lateinit var response: HttpResponse<String>
        webClient.get(endpoint)
            .addQueryParam(paramName, paramValue)
            .addQueryParam(paramName2, paramValue2)
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

    private fun sendGetRequest(latch: CountDownLatch, endpoint: String): HttpResponse<String> {
        val responseLatch = CountDownLatch(1)
        lateinit var response: HttpResponse<String>
        webClient.get(endpoint)
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

    @Timeout(5 * 60)
    @Test
    fun getFriendshipWithoutUsersParam() {
        val latch = CountDownLatch(2)

        val response1 = sendGetRequest("to", friendship1.user1.id.value, latch, Endpoint.FRIENDSHIP)
        val response2 = sendGetRequest("from", friendship1.user2.id.value, latch, Endpoint.FRIENDSHIP)

        latch.await()
        assertAll(
            { assertEquals(StatusCode.BAD_REQUEST, response1.statusCode()) },
            { assertEquals(StatusCode.BAD_REQUEST, response2.statusCode()) }
        )
    }

    @Timeout(5 * 60)
    @Test
    fun getFriendshipRequestWithoutUsersParam() {
        val latch = CountDownLatch(1)

        val response1 = sendGetRequest("to", friendshipRequest1.to.id.value, latch, Endpoint.FRIENDSHIP_REQUEST)
        val response2 = sendGetRequest("from", friendshipRequest1.from.id.value, latch, Endpoint.FRIENDSHIP_REQUEST)

        latch.await()
        assertAll(
            { assertEquals(StatusCode.BAD_REQUEST, response1.statusCode()) },
            { assertEquals(StatusCode.BAD_REQUEST, response2.statusCode()) }
        )
    }

    @Timeout(5 * 60)
    @Test
    fun getMessageWithWrongParamFromMessageReceiveEndpoint() {
        val latch = CountDownLatch(1)

        val response = sendGetRequest("wrong_parameter", message1.sender.id.value, latch, Endpoint.MESSAGE_RECEIVE)

        latch.await()
        assertEquals(StatusCode.BAD_REQUEST, response.statusCode())
    }

    @Timeout(5 * 60)
    @Test
    fun getAllMessagesFromReceiveEndpoint() {
        val latch = CountDownLatch(1)

        // adds users, friendship request, friendship and message to the database to be able to get all messages
        service.addUser(user1)
        service.addUser(user2)
        service.addFriendshipRequest(friendshipRequest1)
        service.addFriendship(friendship1)
        listOf(message1, message2, message3).forEach { service.receivedMessage(it) }

        val response = sendGetRequest("id", user2.id.value, latch, Endpoint.MESSAGE_RECEIVE)

        val actual = mapper.readValue(response.body(), Array<Message>::class.java)
        val expected = arrayOf(message1, message2, message3)

        latch.await()
        assertAll(
            { assertEquals(StatusCode.OK, response.statusCode()) },
            { assertEquals(expected.size, actual.size) }
        )
    }

    @Timeout(5 * 60)
    @Test
    fun getAllMessagesFromReceiveEndpointWithoutParameters() {
        val latch = CountDownLatch(1)

        // adds users, friendship request, friendship and message to the database to be able to get all messages
        service.addUser(user1)
        service.addUser(user2)
        service.addFriendshipRequest(friendshipRequest1)
        service.addFriendship(friendship1)
        listOf(message1, message2, message3).forEach { service.receivedMessage(it) }

        val response = sendGetRequest(latch, Endpoint.MESSAGE_RECEIVE)

        latch.await()
        assertEquals(StatusCode.BAD_REQUEST, response.statusCode())
    }

    @Timeout(5 * 60)
    @Test
    fun getAllFriendshipRequestsWithoutParameters() {
        val latch = CountDownLatch(1)

        // adds users and friendship request to the database to be able to get all friendship requests
        listOf(user1, user2, user3, user4, user5, user6).forEach { service.addUser(it) }
        listOf(friendshipRequest1, friendshipRequest2, friendshipRequest3).forEach { service.addFriendshipRequest(it) }

        val response = sendGetRequest(latch, Endpoint.FRIENDSHIP_REQUEST)

        latch.await()
        assertEquals(StatusCode.BAD_REQUEST, response.statusCode())
    }

    @Timeout(5 * 60)
    @Test
    fun getFriendshipRequests() {
        val latch = CountDownLatch(1)

        // adds users and friendship request to the database to be able to get all friendship requests
        listOf(user1, user2, user3, user4, user5, user6).forEach { service.addUser(it) }
        listOf(friendshipRequest1, friendshipRequest2, friendshipRequest3).forEach { service.addFriendshipRequest(it) }

        val response = sendGetRequest("id", user1.id.value, latch, Endpoint.FRIENDSHIP_REQUEST)

        val actual = mapper.readValue(response.body(), Array<FriendshipRequest>::class.java)
        val expected = arrayOf(friendshipRequest1)

        latch.await()
        assertAll(
            { assertEquals(StatusCode.OK, response.statusCode()) },
            { assertEquals(actual.size, expected.size) }
        )
    }

    @Timeout(5 * 60)
    @Test
    fun getFriendshipsWithWrongParams() {
        val latch = CountDownLatch(1)

        // adds users, friendship request and friendship to the database to be able to get all friendships
        listOf(user1, user2, user3, user4, user5, user6).forEach { service.addUser(it) }
        listOf(friendshipRequest1, friendshipRequest2, friendshipRequest3).forEach { service.addFriendshipRequest(it) }
        listOf(friendship1, friendship2, friendship3).forEach { service.addFriendship(it) }

        val response = sendGetRequest("wrongParamName", user1.id.value, latch, Endpoint.FRIENDSHIP)

        latch.await()
        assertEquals(StatusCode.BAD_REQUEST, response.statusCode())
    }

    @Timeout(5 * 60)
    @Test
    fun getFriendships() {
        val latch = CountDownLatch(1)

        // adds users, friendship request and friendship to the database to be able to get all friendships
        listOf(user1, user2, user3, user4, user5, user6).forEach { service.addUser(it) }
        listOf(friendshipRequest1, friendshipRequest2, friendshipRequest3).forEach { service.addFriendshipRequest(it) }
        listOf(friendship1, friendship2, friendship3).forEach { service.addFriendship(it) }

        val response = sendGetRequest("id", user1.id.value, latch, Endpoint.FRIENDSHIP)

        val actual = mapper.readValue(response.body(), Array<User>::class.java)
        val expected = arrayOf(friendship1)

        latch.await()
        assertAll(
            { assertEquals(StatusCode.OK, response.statusCode()) },
            { assertEquals(actual.size, expected.size) }
        )
    }

    @Timeout(5 * 60)
    @Test
    fun getChatWithWrongParameter() {
        val latch = CountDownLatch(1)

        // adds users, friendship request, friendship and message to the database to be able to get a chat
        service.addUser(user1)
        service.addUser(user2)
        service.addFriendshipRequest(friendshipRequest1)
        service.addFriendship(friendship1)
        listOf(message1, message2, message3).forEach { service.receivedMessage(it) }

        val response = sendGetRequest("wrongParameter", user2.id.value, latch, Endpoint.MESSAGE_CHAT)

        latch.await()
        assertEquals(StatusCode.BAD_REQUEST, response.statusCode())
    }

    @Timeout(5 * 60)
    @Test
    fun getChat() {
        val latch = CountDownLatch(1)

        // adds users, friendship request, friendship and message to the database to be able to get a chat
        service.addUser(user1)
        service.addUser(user2)
        service.addFriendshipRequest(friendshipRequest1)
        service.addFriendship(friendship1)
        listOf(message1, message2, message3).forEach { service.receivedMessage(it) }

        val response = sendGetRequest("user1Id", user1.id.value, "user2Id", user2.id.value, latch, Endpoint.MESSAGE_CHAT)

        val actual = mapper.readValue(response.body(), Array<Message>::class.java)
        val expected = arrayOf(message1, message2, message3)

        latch.await()
        assertAll(
            { assertEquals(StatusCode.OK, response.statusCode()) },
            { assertEquals(expected.size, actual.size) }
        )
    }

    @Timeout(5 * 60)
    @Test
    fun getChatWithoutParameters() {
        val latch = CountDownLatch(1)

        // adds users, friendship request, friendship and message to the database to be able to get a chat
        service.addUser(user1)
        service.addUser(user2)
        service.addFriendshipRequest(friendshipRequest1)
        service.addFriendship(friendship1)
        listOf(message1, message2, message3).forEach { service.receivedMessage(it) }

        val response = sendGetRequest(latch, Endpoint.MESSAGE_CHAT)

        latch.await()
        assertEquals(StatusCode.BAD_REQUEST, response.statusCode())
    }

    private fun sendPutRequest(
        send: JsonObject?,
        latch: CountDownLatch,
        endpoint: String
    ): HttpResponse<String> {
        val responseLatch = CountDownLatch(1)
        lateinit var response: HttpResponse<String>
        webClient.put(endpoint)
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

    @Timeout(5 * 60)
    @Test
    fun acceptUnexistingFriendshipRequest() {
        val latch = CountDownLatch(1)

        val friendshipRequestJsonString = mapper.writeValueAsString(friendshipRequest1)
        val friendshipRequestJson = JsonObject(friendshipRequestJsonString)

        val response = sendPutRequest(friendshipRequestJson, latch, Endpoint.FRIENDSHIP_REQUEST_ACCEPT)

        latch.await()
        assertEquals(StatusCode.BAD_REQUEST, response.statusCode())
    }

    @Timeout(5 * 60)
    @Test
    fun acceptFriendshipRequest() {
        val latch = CountDownLatch(3)

        // adds users and friendship request to the database to be able to accept a friendship request
        service.addUser(user1)
        service.addUser(user2)
        service.addFriendshipRequest(friendshipRequest1)

        val friendshipRequestJsonString = mapper.writeValueAsString(friendshipRequest1)
        val friendshipRequestJson = JsonObject(friendshipRequestJsonString)

        val getFriendshipResponseBeforeUpdate = sendGetRequest("id", user1.id.value, latch, Endpoint.FRIENDSHIP)
        val putFriendshipResponse = sendPutRequest(friendshipRequestJson, latch, Endpoint.FRIENDSHIP_REQUEST_ACCEPT)
        val getFriendshipResponseAfterUpdate = sendGetRequest("id", user1.id.value, latch, Endpoint.FRIENDSHIP)

        val getUserFriendshipAfterUpdate = mapper.readValue(getFriendshipResponseAfterUpdate.body(), Array<User>::class.java)
        val expectedGetUserFriendshipAfterUpdate = arrayOf(user2)

        latch.await()
        assertAll(
            { assertEquals(StatusCode.OK, getFriendshipResponseBeforeUpdate.statusCode()) },
            { assertEquals(getFriendshipResponseBeforeUpdate.body(), "[]") },
            { assertEquals(StatusCode.OK, putFriendshipResponse.statusCode()) },
            { assertEquals(StatusCode.OK, getFriendshipResponseAfterUpdate.statusCode()) },
            { assertEquals(getUserFriendshipAfterUpdate.size, 1) },
            { assertEquals(expectedGetUserFriendshipAfterUpdate.first(), getUserFriendshipAfterUpdate.first()) },
        )
    }

    @Timeout(5 * 60)
    @Test
    fun rejectFriendshipRequest() {
        val latch = CountDownLatch(3)

        // adds users and friendship request to the database to be able to accept a friendship request
        service.addUser(user1)
        service.addUser(user2)
        service.addFriendshipRequest(friendshipRequest1)

        val friendshipRequestJsonString = mapper.writeValueAsString(friendshipRequest1)
        val friendshipRequestJson = JsonObject(friendshipRequestJsonString)

        val getFriendshipRequestResponseBeforeUpdate = sendGetRequest("id", user1.id.value, latch, Endpoint.FRIENDSHIP_REQUEST)
        val friendshipRequestBeforeUpdate = mapper.readValue(getFriendshipRequestResponseBeforeUpdate.body(), Array<FriendshipRequest>::class.java)
        val expectedGetFriendshipRequestBeforeUpdate = arrayOf(friendshipRequest1)

        val putFriendshipRequestResponse = sendPutRequest(friendshipRequestJson, latch, Endpoint.FRIENDSHIP_REQUEST_DECLINE)
        val getFriendshipRequestResponseAfterUpdate = sendGetRequest("id", user1.id.value, latch, Endpoint.FRIENDSHIP_REQUEST)

        latch.await()
        assertAll(
            { assertEquals(StatusCode.OK, getFriendshipRequestResponseBeforeUpdate.statusCode()) },
            { assertEquals(friendshipRequestBeforeUpdate.size, 1) },
            { assertEquals(expectedGetFriendshipRequestBeforeUpdate.first(), friendshipRequestBeforeUpdate.first()) },
            { assertEquals(StatusCode.OK, putFriendshipRequestResponse.statusCode()) },
            { assertEquals(StatusCode.OK, getFriendshipRequestResponseAfterUpdate.statusCode()) },
            { assertEquals(getFriendshipRequestResponseAfterUpdate.body(), "[]") },
        )
    }

    @Timeout(5 * 60)
    @Test
    fun rejectUnexistingFriendshipRequest() {
        val latch = CountDownLatch(1)

        val friendshipRequestJsonString = mapper.writeValueAsString(friendshipRequest1)
        val friendshipRequestJson = JsonObject(friendshipRequestJsonString)

        val response = sendPutRequest(friendshipRequestJson, latch, Endpoint.FRIENDSHIP_REQUEST_DECLINE)

        latch.await()
        assertEquals(StatusCode.BAD_REQUEST, response.statusCode())
    }
}
