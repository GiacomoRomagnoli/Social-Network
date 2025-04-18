package test.friendship.infrastructure.persistence.sql

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import social.friendship.domain.User
import social.friendship.infrastructure.persistence.sql.UserSQLRepository
import test.friendship.infrastructure.DockerSQLTest
import java.io.File
import java.sql.SQLIntegrityConstraintViolationException

class UserSQLRepositoryTest : DockerSQLTest() {
    private val userTo = User.of("userToID")
    private val userFrom = User.of("userFromID")
    private val repository = UserSQLRepository()
    private val dockerComposePath = "/social/friendship/infrastructure/persistence/sql/docker-compose.yml"
    private lateinit var dockerComposeFile: File

    @BeforeEach
    fun setUp() {
        val dockerComposeResource = this::class.java.getResource(dockerComposePath) ?: throw Exception("Resource not found")
        dockerComposeFile = File(dockerComposeResource.toURI())
        executeDockerComposeCmd(dockerComposeFile, "up", "--wait")
        repository.connect(localhostIP, port, database, user, password)
    }

    @AfterEach
    fun tearDown() {
        executeDockerComposeCmd(dockerComposeFile, "down", "-v")
    }

    @Timeout(5 * 60)
    @Test
    fun save() {
        repository.save(userTo)
        val actual = repository.findById(userTo.id)
        assertEquals(userTo, actual)
    }

    @Timeout(5 * 60)
    @Test
    fun doesNotSaveDoubles() {
        repository.save(userTo)
        assertThrows<SQLIntegrityConstraintViolationException> {
            repository.save(userTo)
        }
    }

    @Timeout(5 * 60)
    @Test
    fun deleteById() {
        repository.save(userTo)
        val actual = repository.deleteById(userTo.id)
        assertEquals(userTo, actual)
    }

    @Timeout(5 * 60)
    @Test
    fun deleteByIdReturnsNullIfNotFound() {
        val actual = repository.deleteById(userTo.id)
        assertEquals(null, actual)
    }

    @Timeout(5 * 60)
    @Test
    fun findAll() {
        repository.save(userTo)
        repository.save(userFrom)
        val users = repository.findAll().toList()
        assertAll(
            { assertTrue(users.size == 2) },
            { assertTrue(users.contains(userTo)) },
            { assertTrue(users.contains(userFrom)) },
        )
    }
}
