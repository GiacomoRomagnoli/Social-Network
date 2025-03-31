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

    @Test
    @Timeout(5 * 60)
    fun `bob sends to alice a friend request and she accepts`() {
        val bobToken = login(bob)
        val friendshipRequestSent = post(client, Endpoint.FRIENDSHIP_REQUEST_SEND, friendship, bobToken)
        val aliceToken = login(alice)
        val friendshipAccepted = put(client, Endpoint.FRIENDSHIP_REQUEST_ACCEPT, friendship, aliceToken)
        val friendshipCreated = get(client, "${Endpoint.FRIENDSHIP}/${bob.getString("email")}", bobToken)
        val bobFriends = JsonArray(friendshipCreated.body())
        assertEquals(StatusCode.CREATED, friendshipRequestSent.statusCode())
        assertEquals(StatusCode.OK, friendshipAccepted.statusCode())
        assertEquals(StatusCode.OK, friendshipCreated.statusCode())
        assertEquals(1, bobFriends.count())
        assertEquals(
            alice.getString("email"),
            bobFriends.getJsonObject(0).getJsonObject("id").getString("value")
        )
    }
}
