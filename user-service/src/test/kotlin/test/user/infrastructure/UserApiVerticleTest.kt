package test.user.infrastructure

import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.codec.BodyCodec
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.Mockito.mock
import social.common.endpoint.Endpoint
import social.common.endpoint.StatusCode
import social.user.application.UserServiceImpl
import social.user.domain.User
import social.user.domain.UserID
import social.user.infrastructure.controller.rest.UserApiVerticle
import social.user.infrastructure.persitence.sql.UserSQLRepository
import social.user.infrastructure.serialization.jackson.Mapper
import social.utils.docker.DockerTest
import java.io.File
import java.util.concurrent.CountDownLatch

class UserApiVerticleTest : DockerTest() {
    private val logger = LogManager.getLogger(this::class)
    private val userJson = JsonObject()
        .put("email", "test@gmail.com")
        .put("username", "social/user/test")
    private val repository = UserSQLRepository()
    private val service = UserServiceImpl(repository, mock())
    private val api = UserApiVerticle(service)
    private val dockerComposePath = "/social/user/infrastructure/persistence/docker-compose.yml"
    private lateinit var webClient: WebClient
    private lateinit var dockerComposeFile: File
    private lateinit var vertx: Vertx

    @BeforeEach
    fun setUp() {
        val dockerComposeResource = this::class.java.getResource(dockerComposePath) ?: throw Exception("Resource not found")
        dockerComposeFile = File(dockerComposeResource.toURI())

        executeDockerComposeCmd(dockerComposeFile, "up", "--wait")
        repository.connect("127.0.0.1", "3306", "user", "test_user", "password")

        this.vertx = Vertx.vertx()
        deployVerticle(vertx, this.api)

        webClient = WebClient.create(vertx, WebClientOptions().setDefaultPort(8080).setDefaultHost("localhost"))
    }

    private fun deployVerticle(
        vertx: Vertx,
        verticle: Verticle
    ) {
        val latch = CountDownLatch(1)
        vertx.deployVerticle(verticle).onComplete {
            latch.countDown()
            if (it.succeeded()) {
                logger.info("Verticle '{}' started", verticle.javaClass.simpleName)
            } else {
                logger.error("Failed to start verticle '{}': '{}'", verticle.javaClass.simpleName, it.cause().message)
            }
        }
        latch.await()
    }

    @AfterEach
    fun tearDown() {
        // stops and removes the container, also removes the volumes in order to start fresh each time
        executeDockerComposeCmd(dockerComposeFile, "down", "-v")

        val latch = CountDownLatch(1)
        vertx.close().onComplete {
            if (it.succeeded()) {
                logger.info("Vert.x instance closed")
            } else {
                logger.error("Failed to close Vert.x instance:", it.cause())
            }
            latch.countDown()
        }
    }

    @Timeout(5 * 60)
    @Test
    fun addValidUser() {
        lateinit var response: HttpResponse<String>

        val latch = CountDownLatch(1)
        webClient.post(Endpoint.USER)
            .putHeader("Content-Type", "application/json")
            .`as`(BodyCodec.string())
            .sendJsonObject(userJson) {
                response = it.result()
                latch.countDown()
            }
        latch.await()
        assertEquals(StatusCode.CREATED, response.statusCode())
    }

    @Timeout(5 * 60)
    @Test
    fun addUserWithNonValidEmail() {
        lateinit var response: HttpResponse<String>

        val latch = CountDownLatch(1)
        webClient.post(Endpoint.USER)
            .putHeader("Content-Type", "application/json")
            .`as`(BodyCodec.string())
            .sendJsonObject(userJson.copy().put("email", "not-a-valid-email")) {
                response = it.result()
                latch.countDown()
            }
        latch.await()
        assertEquals(StatusCode.BAD_REQUEST, response.statusCode())
    }

    @Timeout(5 * 60)
    @Test
    fun addUserWithMissingEmail() {
        lateinit var response: HttpResponse<String>

        val latch = CountDownLatch(1)
        webClient.post(Endpoint.USER)
            .putHeader("Content-Type", "application/json")
            .`as`(BodyCodec.string())
            .sendJsonObject(userJson.copy().apply { remove("email") }) {
                response = it.result()
                latch.countDown()
            }
        latch.await()
        assertEquals(StatusCode.BAD_REQUEST, response.statusCode())
    }

    @Timeout(5 * 60)
    @Test
    fun addUserWithMissingUsername() {
        lateinit var response: HttpResponse<String>

        val latch = CountDownLatch(1)
        webClient.post(Endpoint.USER)
            .putHeader("Content-Type", "application/json")
            .`as`(BodyCodec.string())
            .sendJsonObject(userJson.copy().apply { remove("username") }) {
                response = it.result()
                latch.countDown()
            }
        latch.await()
        assertEquals(StatusCode.BAD_REQUEST, response.statusCode())
    }

    @Timeout(5 * 60)
    @Test
    fun addDuplicateUser() {
        repository.save(Mapper.readValue(userJson.toString(), User::class.java))
        lateinit var response: HttpResponse<String>

        val latch = CountDownLatch(1)
        webClient.post(Endpoint.USER)
            .putHeader("Content-Type", "application/json")
            .`as`(BodyCodec.string())
            .sendJsonObject(userJson.copy().put("username", "test2")) {
                response = it.result()
                latch.countDown()
            }
        latch.await()
        assertEquals(StatusCode.FORBIDDEN, response.statusCode())
    }

    @Timeout(5 * 60)
    @Test
    fun getUser() {
        repository.save(Mapper.readValue(userJson.toString(), User::class.java))
        lateinit var response: HttpResponse<String>

        val latch = CountDownLatch(1)
        webClient.get(Endpoint.USER)
            .addQueryParam("email", "test@gmail.com")
            .`as`(BodyCodec.string())
            .send {
                response = it.result()
                latch.countDown()
            }
        latch.await()
        assertEquals(StatusCode.OK, response.statusCode())

        val user = Mapper.readValue(response.body(), User::class.java)
        assertEquals(userJson.getString("email"), user.email)
        assertEquals(userJson.getString("username"), user.username)
        assertFalse(user.isBlocked)
        assertFalse(user.isAdmin)
    }

    @Timeout(5 * 60)
    @Test
    fun getUserWithoutEmailParam() {
        lateinit var response: HttpResponse<String>
        val latch = CountDownLatch(1)

        webClient.get(Endpoint.USER)
            .putHeader("content-type", "application/json")
            .`as`(BodyCodec.string())
            .send { ar ->
                latch.countDown()
                if (ar.succeeded()) {
                    response = ar.result()
                } else {
                    fail("Failed to get user: '${ar.cause().message}'")
                }
            }

        latch.await()
        assertEquals(StatusCode.BAD_REQUEST, response.statusCode())
    }

    @Timeout(5 * 60)
    @Test
    fun getNonExistingUser() {
        lateinit var response: HttpResponse<String>
        val latch = CountDownLatch(1)

        webClient.get(Endpoint.USER)
            .addQueryParam("email", "test@gmail.com")
            .putHeader("content-type", "application/json")
            .`as`(BodyCodec.string())
            .send { ar ->
                latch.countDown()
                if (ar.succeeded()) {
                    response = ar.result()
                } else {
                    fail("Failed to get user: '${ar.cause().message}'")
                }
            }

        latch.await()
        assertEquals(StatusCode.NOT_FOUND, response.statusCode())
    }

    @Test
    @Timeout(5 * 60)
    fun blockAndUnblockUser() {
        repository.save(Mapper.readValue(userJson.toString(), User::class.java))
        lateinit var response: HttpResponse<String>

        var latch = CountDownLatch(1)
        webClient.post(Endpoint.BLOCK_USER.replace(":email", userJson.getString("email")))
            .putHeader("Content-Type", "application/json")
            .`as`(BodyCodec.string())
            .send {
                response = it.result()
                latch.countDown()
            }
        latch.await()
        assertEquals(StatusCode.NO_CONTENT, response.statusCode())

        latch = CountDownLatch(1)
        webClient.get(Endpoint.USER)
            .addQueryParam("email", userJson.getString("email"))
            .`as`(BodyCodec.string())
            .send {
                response = it.result()
                latch.countDown()
            }
        latch.await()
        assertEquals(StatusCode.OK, response.statusCode())

        var user = Mapper.readValue(response.body(), User::class.java)
        assertEquals(userJson.getString("email"), user.email)
        assertTrue(user.isBlocked)

        latch = CountDownLatch(1)
        webClient.post(Endpoint.UNBLOCK_USER.replace(":email", userJson.getString("email")))
            .putHeader("Content-Type", "application/json")
            .`as`(BodyCodec.string())
            .send {
                response = it.result()
                latch.countDown()
            }
        latch.await()
        assertEquals(StatusCode.NO_CONTENT, response.statusCode())

        latch = CountDownLatch(1)
        webClient.get(Endpoint.USER)
            .addQueryParam("email", userJson.getString("email"))
            .`as`(BodyCodec.string())
            .send {
                response = it.result()
                latch.countDown()
            }
        latch.await()
        assertEquals(StatusCode.OK, response.statusCode())

        user = Mapper.readValue(response.body(), User::class.java)
        assertEquals(userJson.getString("email"), user.email)
        assertFalse(user.isBlocked)
    }

    @Timeout(5 * 60)
    @Test
    fun deleteUser() {
        repository.save(Mapper.readValue(userJson.toString(), User::class.java))
        lateinit var response: HttpResponse<String>

        val latch = CountDownLatch(1)
        webClient.delete("${Endpoint.USER}/${userJson.getString("email")}")
            .`as`(BodyCodec.string())
            .send {
                latch.countDown()
                if (it.succeeded()) {
                    response = it.result()
                }
            }
        latch.await()
        assertEquals(StatusCode.NO_CONTENT, response.statusCode())
        assertNull(repository.findById(UserID.of(userJson.getString("email"))))
    }
}
