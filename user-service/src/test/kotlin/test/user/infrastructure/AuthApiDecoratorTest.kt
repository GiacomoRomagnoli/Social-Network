package test.user.infrastructure

import io.jsonwebtoken.Jwts
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.codec.BodyCodec
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.Mockito.mock
import social.common.endpoint.Endpoint
import social.common.endpoint.StatusCode
import social.user.application.AuthService
import social.user.application.AuthServiceImpl
import social.user.application.CredentialsRepository
import social.user.application.UserServiceImpl
import social.user.domain.Credentials
import social.user.domain.User
import social.user.domain.UserID
import social.user.infrastructure.controller.rest.AuthApiDecorator
import social.user.infrastructure.persitence.sql.CredentialsSQLRepository
import social.user.infrastructure.persitence.sql.SQLUtils
import social.user.infrastructure.persitence.sql.UserSQLRepository
import social.utils.docker.DockerTest
import java.io.File
import java.util.concurrent.CountDownLatch

class AuthApiDecoratorTest : DockerTest() {
    private val userRepository = UserSQLRepository()
    private val userService = UserServiceImpl(userRepository, mock())
    private lateinit var credentialsRepository: CredentialsRepository
    private lateinit var authService: AuthService
    private lateinit var server: AuthApiDecorator
    private lateinit var client: WebClient
    private lateinit var dockerComposeFile: File
    private lateinit var vertx: Vertx
    private val dockerComposePath = "/social/user/infrastructure/persistence/docker-compose.yml"

    @BeforeEach
    fun setUp() {
        val dockerComposeResource = this::class.java.getResource(dockerComposePath) ?: throw Exception("Resource not found")
        dockerComposeFile = File(dockerComposeResource.toURI())
        executeDockerComposeCmd(dockerComposeFile, "up", "--wait")
        userRepository.connect(
            "127.0.0.1",
            "3306",
            "user",
            "test_user",
            "password"
        )
        credentialsRepository = CredentialsSQLRepository(
            SQLUtils.mySQLConnection(
                "127.0.0.1",
                "3306",
                "user",
                "test_user",
                "password"
            )
        )
        authService = AuthServiceImpl(credentialsRepository, userRepository, mock())
        server = AuthApiDecorator(userService, authService)
        vertx = Vertx.vertx()
        deployVerticle(vertx, server)
        client = WebClient.create(
            vertx,
            WebClientOptions()
                .setDefaultPort(8080)
                .setDefaultHost("localhost")
        )
    }

    private fun deployVerticle(
        vertx: Vertx,
        verticle: Verticle
    ) {
        val latch = CountDownLatch(1)
        vertx.deployVerticle(verticle).onComplete {
            latch.countDown()
            if (it.failed()) {
                throw it.cause()
            }
        }
        latch.await()
    }

    @AfterEach
    fun tearDown() {
        executeDockerComposeCmd(dockerComposeFile, "down", "-v")
        val latch = CountDownLatch(1)
        vertx.close().onComplete {
            if (it.failed()) {
                println("fail to close vertx")
            }
            latch.countDown()
        }
    }

    @Timeout(5 * 60)
    @Test
    fun loginWithJWT() {
        userRepository.save(User.of("test@gmail.com", "test"))
        credentialsRepository.save(Credentials.of("test@gmail.com", "1ValidPassword!"))
        lateinit var response: HttpResponse<String>
        val latch = CountDownLatch(1)
        val body = JsonObject()
            .put("email", "test@gmail.com")
            .put("password", "1ValidPassword!")
        client.post(Endpoint.LOGIN)
            .putHeader("content-type", "application/json")
            .`as`(BodyCodec.string())
            .sendJsonObject(body) {
                latch.countDown()
                if (it.succeeded()) {
                    response = it.result()
                }
            }
        latch.await()
        val subject = Jwts.parser()
            .verifyWith((authService as AuthServiceImpl).publicKey)
            .build()
            .parseSignedClaims(response.body())
            .payload
            .subject
        assertEquals(StatusCode.OK, response.statusCode())
        assertEquals(subject, body.getString("email"))
    }

    @Timeout(5 * 60)
    @Test
    fun addCredentialsForExistingUser() {
        lateinit var response: HttpResponse<String>
        val latch = CountDownLatch(1)
        val body = JsonObject()
            .put("email", "test@gmail.com")
            .put("password", "1ValidPassword!")
        userRepository.save(User.of("test@gmail.com", "test"))
        client.post(Endpoint.CREDENTIALS)
            .`as`(BodyCodec.string())
            .sendJsonObject(body) {
                latch.countDown()
                if (it.succeeded()) {
                    response = it.result()
                }
            }
        latch.await()
        assertEquals(StatusCode.CREATED, response.statusCode())
        assertTrue(
            credentialsRepository.findById(UserID.of("test@gmail.com"))
            !!.password.match("1ValidPassword!")
        )
    }
}
