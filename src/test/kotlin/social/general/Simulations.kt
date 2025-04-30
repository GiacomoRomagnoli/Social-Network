package social.general

import io.vertx.core.Vertx
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.WebSocketConnectOptions
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
import social.common.endpoint.Endpoint.EMAIL_PARAM
import social.common.endpoint.Port
import social.common.endpoint.StatusCode
import social.common.events.FriendshipRequestSent
import social.common.events.MessageSent
import social.utils.docker.DockerTest
import social.utils.http.TestRequestUtils.get
import social.utils.http.TestRequestUtils.post
import social.utils.http.TestRequestUtils.put
import java.io.File
import java.util.concurrent.CountDownLatch

class Simulations : DockerTest() {
    private val admin = JsonObject()
        .put("email", "admin@social.com")
        .put("password", "AdminTest123!")
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
    private val post = JsonObject()
        .put(
            "user",
            JsonObject()
                .put("email", bob.getString("email"))
                .put("name", bob.getString("username"))
        )
        .put("content", "Hello world!")
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

    @Test
    @Timeout(5 * 60)
    fun `bob publishes a new post and alice reads it from her feed`() {
        val bobToken = login(bob)
        val aliceToken = login(alice)

        val friendshipRequestSent = eventually {
            val req = post(client, Endpoint.FRIENDSHIP_REQUEST_SEND, friendship, bobToken)
            if (req.statusCode() == StatusCode.FORBIDDEN) null else req
        }
        assertEquals(StatusCode.CREATED, friendshipRequestSent.statusCode())

        val friendshipAccepted = put(client, Endpoint.FRIENDSHIP_REQUEST_ACCEPT, friendship, aliceToken)
        assertEquals(StatusCode.OK, friendshipAccepted.statusCode())

        val postPublished = post(client, Endpoint.POST, post, bobToken)
        assertEquals(StatusCode.CREATED, postPublished.statusCode())

        val feedReceived = get(client, "${Endpoint.POST}/feed/${alice.getString("email")}", aliceToken)
        assertEquals(StatusCode.OK, feedReceived.statusCode())

        val feed = JsonObject(feedReceived.body())
        assertEquals(alice.getString("email"), feed.getJsonObject("owner").getString("email"))

        val posts = feed.getJsonArray("posts")
        assertEquals(1, posts.size())

        val bobPost = posts.getJsonObject(0)
        assertEquals(bob.getString("email"), bobPost.getJsonObject("author").getString("email"))
        assertEquals(post.getString("content"), bobPost.getString("content"))
    }

    @Test
    @Timeout(5 * 60)
    fun `bob has been blocked so he can't publish any post`() {
        val adminToken = login(admin)
        val userBlocked = post(
            client,
            Endpoint.BLOCK_USER.replace(":$EMAIL_PARAM", bob.getString("email")),
            JsonObject(),
            adminToken
        )
        assertEquals(StatusCode.NO_CONTENT, userBlocked.statusCode())

        val bobToken = login(bob)
        val postPublished = post(client, Endpoint.POST, post, bobToken)
        assertEquals(StatusCode.UNAUTHORIZED, postPublished.statusCode())
    }

    @Test
    @Timeout(5 * 60)
    fun `admin checks total user number`() {
        val adminToken = login(admin)
        val userCount = get(client, Endpoint.USER_COUNT, adminToken)
        println(userCount.body())
        assertEquals(StatusCode.OK, userCount.statusCode())
        assertEquals("3", userCount.body())
    }

    @Test
    @Timeout(3 * 60)
    fun `bob and alice become friends and bob send a message to alice and she receive notifications`() {
        lateinit var event: JsonObject
        val requestSent = CountDownLatch(1)
        val messageSent = CountDownLatch(1)
        val aliceAuth = CountDownLatch(1)
        val wsClient = vertx.createHttpClient(HttpClientOptions().setSsl(true).setTrustAll(true).setVerifyHost(false))
        val options: WebSocketConnectOptions = WebSocketConnectOptions()
            .setURI("/")
            .setHost("localhost")
            .setPort(8081)
        val bobToken = login(bob)
        val aliceToken = login(alice)

        wsClient.webSocket(options).onSuccess { ws ->
            ws.textMessageHandler {
                event = JsonObject(it)
                println("socket: \n$event")
                when (event.getString("type")) {
                    MessageSent.TOPIC -> messageSent.countDown()
                    FriendshipRequestSent.TOPIC -> requestSent.countDown()
                    else -> aliceAuth.countDown()
                }
            }
            ws.writeTextMessage(aliceToken)
        }.onFailure { println("web socket failed"); aliceAuth.countDown() }
        aliceAuth.await()
        assertEquals("authenticated", event.getString("type"))

        val friendshipRequestSent = eventually {
            val req = post(client, Endpoint.FRIENDSHIP_REQUEST_SEND, friendship, bobToken)
            if (req.statusCode() == StatusCode.FORBIDDEN) null else req
        }
        assertEquals(StatusCode.CREATED, friendshipRequestSent.statusCode())

        requestSent.await()
        assertEquals(bob.getString("email"), event.getString("sender"))
        assertEquals(alice.getString("email"), event.getString("receiver"))

        val friendshipAccepted = put(client, Endpoint.FRIENDSHIP_REQUEST_ACCEPT, friendship, aliceToken)
        assertEquals(StatusCode.OK, friendshipAccepted.statusCode())

        val message1Sent = post(client, Endpoint.MESSAGE_SEND, message1, bobToken)
        assertEquals(StatusCode.CREATED, message1Sent.statusCode())

        println("awaiting...")
        messageSent.await()
        println("done!")
        assertEquals(bob.getString("email"), event.getString("sender"))
        assertEquals(alice.getString("email"), event.getString("receiver"))
        assertEquals(message1.getString("content"), event.getString("message"))
    }
}
