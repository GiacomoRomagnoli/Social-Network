package test.user.infrastructure.persitence

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import social.user.application.CredentialsRepository
import social.user.domain.Credentials
import social.user.domain.Password
import social.user.domain.User
import social.user.infrastructure.persitence.sql.CredentialsSQLRepository
import social.user.infrastructure.persitence.sql.SQLUtils
import social.user.infrastructure.persitence.sql.UserSQLRepository
import social.utils.docker.DockerTest
import java.io.File
import java.sql.SQLIntegrityConstraintViolationException

class CredentialsSQLRepositoryTest : DockerTest() {
    private val user = User.of("example@domain.org", "example")
    private val password = Password.of("1ValidPassword!")
    private val credentials = Credentials.of(user.id, password)
    private lateinit var credentialsRepository: CredentialsRepository
    private var userRepository = UserSQLRepository()
    private val dockerComposePath = "/social/user/infrastructure/persistence/sql/docker-compose.yml"
    private lateinit var dockerComposeFile: File

    @BeforeEach
    fun setUp() {
        val dockerComposeResource =
            this::class.java.getResource(dockerComposePath) ?: throw Exception("Resource not found")
        dockerComposeFile = File(dockerComposeResource.toURI())
        executeDockerComposeCmd(dockerComposeFile, "up", "--wait")
        credentialsRepository = CredentialsSQLRepository(
            SQLUtils.mySQLConnection(
                "127.0.0.1",
                "3306",
                "user",
                "test_user",
                "password"
            )
        )
        userRepository.connect(
            "127.0.0.1",
            "3306",
            "user",
            "test_user",
            "password"
        )
    }

    @AfterEach
    fun tearDown() {
        executeDockerComposeCmd(dockerComposeFile, "down", "-v")
    }

    @Timeout(5 * 60)
    @Test
    fun save() {
        userRepository.save(user)
        credentialsRepository.save(credentials)
        val db = credentialsRepository.findById(user.id)
        assertNotNull(db)
        assertTrue(db == credentials)
        assertTrue(db!!.id.value == credentials.id.value)
        assertTrue(db.password.match(credentials.password))
    }

    @Timeout(5 * 60)
    @Test
    fun missingForeignKeySave() {
        assertThrows<SQLIntegrityConstraintViolationException> {
            credentialsRepository.save(credentials)
        }
    }
}
