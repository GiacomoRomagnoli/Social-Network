package social.general

import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import social.common.endpoint.Endpoint
import social.common.endpoint.Port
import social.common.endpoint.StatusCode
import social.utils.docker.DockerTest
import social.utils.http.TestRequestUtils.get
import social.utils.http.TestRequestUtils.post
import social.utils.http.TestRequestUtils.put
import java.io.File

class Simulations : DockerTest() {
    private val bob = JsonObject()
        .put("email", "bob@test.com")
        .put("username", "bob")
        .put("password", "1ValidPassword!")
    private val alice = JsonObject()
        .put("email", "alice@test.com")
        .put("username", "alice")
        .put("password", "1ValidPassword!")
    private val friendship = JsonObject()
        .put("to", alice.getString("email"))
        .put("from", bob.getString("email"))
    private val message1 = JsonObject()
        .put("sender", bob.getString("email"))
        .put("receiver", alice.getString("email"))
        .put("content", "Hi, Alice!")
    private val message2 = JsonObject()
        .put("sender", bob.getString("email"))
        .put("receiver", alice.getString("email"))
        .put("content", "How are you doing?")
    private val dockerComposePath = "docker-compose.yml"
    private lateinit var dockerComposeFile: File
    private lateinit var client: WebClient
    private lateinit var vertx: Vertx

    @BeforeEach
    fun setUp() {
        dockerComposeFile = File(dockerComposePath)
        executeDockerComposeCmd(dockerComposeFile, "up", "--wait")
        vertx = Vertx.vertx()
        client = WebClient.create(
            vertx,
            WebClientOptions()
                .setSsl(true)
                .setTrustAll(true)
                .setVerifyHost(false)
                .setDefaultPort(Port.HTTP)
                .setDefaultHost("localhost")
        )
        registration(bob)
        registration(alice)
    }

    @AfterEach
    fun tearDown() {
        executeDockerComposeCmd(dockerComposeFile, "down", "-v")
    }

    private fun registration(user: JsonObject) {
        val response = post(client, Endpoint.USER, user)
        require(response.statusCode() == StatusCode.CREATED)
    }

    private fun login(credentials: JsonObject): String {
        val response = post(client, Endpoint.LOGIN, credentials)
        require(response.statusCode() == StatusCode.OK)
        return response.body()
    }

    private fun <T> eventually(timeoutMillis: Long = 2000, pollInterval: Long = 100, block: () -> T?): T {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            val result = block()
            if (result != null) return result
            Thread.sleep(pollInterval)
        }
        throw AssertionError("Condition not met within $timeoutMillis ms")
    }

    @Test
    @Timeout(5 * 60)
    fun `bob sends to alice a friend request and she accepts`() {
        val bobToken = login(bob)
        val aliceToken = login(alice)

        val friendshipRequestSent = eventually {
            val req = post(client, Endpoint.FRIENDSHIP_REQUEST_SEND, friendship, bobToken)
            if (req.statusCode() == StatusCode.FORBIDDEN) null else req
        }
        assertEquals(StatusCode.CREATED, friendshipRequestSent.statusCode())

        val friendshipAccepted = put(client, Endpoint.FRIENDSHIP_REQUEST_ACCEPT, friendship, aliceToken)
        assertEquals(StatusCode.OK, friendshipAccepted.statusCode())

        val friendshipCreated = get(client, "${Endpoint.FRIENDSHIP}/${bob.getString("email")}", bobToken)
        assertEquals(StatusCode.OK, friendshipCreated.statusCode())

        val bobFriends = JsonArray(friendshipCreated.body() ?: "[]")
        assertEquals(1, bobFriends.count())
        assertEquals(alice.getString("email"), bobFriends.getString(0))
    }

    @Test
    @Timeout(5 * 60)
    fun `bob sends his friend alice some messages and she load the chat`() {
        val bobToken = login(bob)
        val aliceToken = login(alice)

        val friendshipRequestSent = eventually {
            val req = post(client, Endpoint.FRIENDSHIP_REQUEST_SEND, friendship, bobToken)
            if (req.statusCode() == StatusCode.FORBIDDEN) null else req
        }
        assertEquals(StatusCode.CREATED, friendshipRequestSent.statusCode())

        val friendshipAccepted = put(client, Endpoint.FRIENDSHIP_REQUEST_ACCEPT, friendship, aliceToken)
        assertEquals(StatusCode.OK, friendshipAccepted.statusCode())

        val message1Sent = post(client, Endpoint.MESSAGE_SEND, message1, bobToken)
        assertEquals(StatusCode.CREATED, message1Sent.statusCode())

        val message2Sent = post(client, Endpoint.MESSAGE_SEND, message2, bobToken)
        assertEquals(StatusCode.CREATED, message2Sent.statusCode())

        val chatRequest = get(
            client,
            "${Endpoint.MESSAGE_CHAT}/${bob.getString("email")}/${alice.getString("email")}",
            aliceToken
        )
        assertEquals(StatusCode.OK, chatRequest.statusCode())

        val chat = JsonArray(chatRequest.body())
        assertEquals(2, chat.size())

        val expected1 = chat.getJsonObject(1)
        val expected2 = chat.getJsonObject(0)
        assertEquals(message1.getString("content"), expected1.getString("content"))
        assertEquals(message1.getString("sender"), expected1.getString("sender"))
        assertEquals(message1.getString("receiver"), expected1.getString("receiver"))
        assertEquals(message2.getString("content"), expected2.getString("content"))
        assertEquals(message2.getString("sender"), expected2.getString("sender"))
        assertEquals(message2.getString("receiver"), expected2.getString("receiver"))
    }
}
