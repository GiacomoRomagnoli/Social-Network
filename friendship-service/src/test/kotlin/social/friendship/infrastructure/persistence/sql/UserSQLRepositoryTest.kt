package social.friendship.infrastructure.persistence.sql

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import social.friendship.infrastructure.DockerSQLTest
import social.friendship.social.friendship.domain.User
import social.friendship.social.friendship.infrastructure.persistence.sql.UserSQLRepository
import java.io.File
import java.sql.SQLIntegrityConstraintViolationException

object UserSQLRepositoryTest : DockerSQLTest() {
    private val userTo = User.of("userToID")
    private val userFrom = User.of("userFromID")
    private val repository = UserSQLRepository()
    private lateinit var dockerComposeFile: File

    @JvmStatic
    @BeforeAll
    fun setUpAll() {
        dockerComposeFile = generateDockerComposeFile()
    }

    @BeforeEach
    fun setUp() {
        executeDockerComposeCmd(dockerComposeFile, "up", "--wait")
        repository.connect(host, port, database, user, password)
    }

    @AfterEach
    fun tearDown() {
        executeDockerComposeCmd(dockerComposeFile, "down", "-v")
    }

    @Timeout(5 * 60)
    @Test
    fun save() {
        repository.save(userTo)
        repository.findById(userTo.id)?.let {
            assertEquals(userTo, it)
        }
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
        repository.deleteById(userTo.id)?.let {
            assertEquals(userTo, it)
        }
    }

    @Timeout(5 * 60)
    @Test
    fun deleteByIdReturnsNullIfNotFound() {
        repository.deleteById(userTo.id)?.let {
            assertEquals(null, it)
        }
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
