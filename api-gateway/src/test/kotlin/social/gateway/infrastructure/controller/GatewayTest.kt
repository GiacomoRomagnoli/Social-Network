package social.gateway.infrastructure.controller

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.codec.BodyCodec
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import social.common.endpoint.Endpoint
import social.common.endpoint.Port
import social.common.endpoint.StatusCode
import social.utils.docker.DockerTest
import java.io.File
import java.util.concurrent.CountDownLatch

class GatewayTest : DockerTest() {
    private val dockerComposePath = "/social/gateway/infrastructure/controller/docker-compose.yml"
    private lateinit var dockerComposeFile: File
    private lateinit var client: WebClient
    private lateinit var vertx: Vertx

    @BeforeEach
    fun setUp() {
        val dockerComposeResource = this::class.java.getResource(dockerComposePath)
            ?: throw Exception("Resource not found")
        dockerComposeFile = File(dockerComposeResource.toURI())
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
    }

    @AfterEach
    fun tearDown() {
        executeDockerComposeCmd(dockerComposeFile, "down", "-v")
    }

    @Test
    fun registrationFlowSuccessful() {
        lateinit var response: HttpResponse<String>
        val latch = CountDownLatch(1)
        val body = JsonObject()
            .put("email", "test@test.com")
            .put("username", "test")
            .put("password", "1ValidPassword!")
        client.post(Endpoint.USER)
            .putHeader("content-type", "application/json")
            .`as`(BodyCodec.string())
            .sendJsonObject(body) {
                if (it.succeeded()) {
                    response = it.result()
                }
                latch.countDown()
            }
        latch.await()
        assertEquals(response.statusCode(), StatusCode.CREATED)
    }

    @Test
    fun registrationFlowBadRequest() {
        lateinit var response: HttpResponse<String>
        val latch = CountDownLatch(1)
        val body = JsonObject()
            .put("email", "test@test.com")
            .put("username", "test")
            .put("password", "invalidPassword")
        client.post(Endpoint.USER)
            .putHeader("content-type", "application/json")
            .`as`(BodyCodec.string())
            .sendJsonObject(body) {
                if (it.succeeded()) {
                    response = it.result()
                }
                latch.countDown()
            }
        latch.await()
        assertEquals(response.statusCode(), StatusCode.BAD_REQUEST)
    }

    @Test
    fun registrationFlowSagaRecovery() {
        lateinit var response: HttpResponse<String>
        lateinit var response2: HttpResponse<String>
        var latch = CountDownLatch(1)
        val body = JsonObject()
            .put("email", "test@test.com")
            .put("username", "test")
            .put("password", "invalidPassword")
        client.post(Endpoint.USER)
            .putHeader("content-type", "application/json")
            .`as`(BodyCodec.string())
            .sendJsonObject(body) {
                if (it.succeeded()) {
                    response = it.result()
                }
                latch.countDown()
            }
        latch.await()
        latch = CountDownLatch(1)
        WebClient.create(vertx).get(8081, "localhost", Endpoint.USER)
            .`as`(BodyCodec.string())
            .addQueryParam("email", "test@test.com")
            .send {
                if (it.succeeded()) {
                    response2 = it.result()
                } else {
                    println(it.cause())
                }
                latch.countDown()
            }
        latch.await()
        assertEquals(StatusCode.BAD_REQUEST, response.statusCode())
        assertEquals(StatusCode.NOT_FOUND, response2.statusCode())
    }

    @Test
    fun loginFlow() {
        // Registration
        lateinit var registration: HttpResponse<String>
        var latch = CountDownLatch(1)
        val user = JsonObject()
            .put("email", "test@test.com")
            .put("username", "test")
            .put("password", "1ValidPassword!")
        client.post(Endpoint.USER)
            .putHeader("content-type", "application/json")
            .`as`(BodyCodec.string())
            .sendJsonObject(user) {
                if (it.succeeded()) {
                    registration = it.result()
                }
                latch.countDown()
            }
        latch.await()
        // Login
        latch = CountDownLatch(1)
        lateinit var login: HttpResponse<String>
        val credentials = JsonObject()
            .put("email", "test@test.com")
            .put("password", "1ValidPassword!")
        client.post(Endpoint.LOGIN)
            .putHeader("content-type", "application/json")
            .`as`(BodyCodec.string())
            .sendJsonObject(credentials) {
                if (it.succeeded()) {
                    login = it.result()
                }
                latch.countDown()
            }
        latch.await()
        // Operation
        latch = CountDownLatch(1)
        lateinit var operation: HttpResponse<String>
        client.get("${Endpoint.USER}/test@test.com")
            .putHeader("Authorization", "Bearer ${login.body()}")
            .`as`(BodyCodec.string())
            .send {
                if (it.succeeded()) {
                    operation = it.result()
                }
                latch.countDown()
            }
        latch.await()
        assertEquals(StatusCode.CREATED, registration.statusCode())
        assertEquals(StatusCode.OK, login.statusCode())
        assertEquals(StatusCode.OK, operation.statusCode())
        assertEquals(user.getString("email"), JsonObject(operation.body()).getString("email"))
        assertEquals(user.getString("username"), JsonObject(operation.body()).getString("username"))
    }
}
