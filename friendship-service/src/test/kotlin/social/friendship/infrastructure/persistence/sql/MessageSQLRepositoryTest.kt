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
import social.friendship.domain.Friendship
import social.friendship.domain.FriendshipRequest
import social.friendship.domain.Message
import social.friendship.infrastructure.DockerSQLTest
import social.friendship.social.friendship.domain.User
import social.friendship.social.friendship.infrastructure.persistence.sql.FriendshipRequestSQLRepository
import social.friendship.social.friendship.infrastructure.persistence.sql.FriendshipSQLRepository
import social.friendship.social.friendship.infrastructure.persistence.sql.MessageSQLRepository
import social.friendship.social.friendship.infrastructure.persistence.sql.UserSQLRepository
import java.io.File
import java.sql.SQLIntegrityConstraintViolationException

object MessageSQLRepositoryTest : DockerSQLTest() {
    private val userTo = User.of("userToID")
    private val userFrom = User.of("userFromID")
    private val userRepository = UserSQLRepository()
    private val friendship = Friendship.of(userTo, userFrom)
    private val friendshipRequestRepository = FriendshipRequestSQLRepository()
    private val friendshipRepository = FriendshipSQLRepository()
    private val message = Message.of(friendship, "content")
    private val message2 = Message.of(friendship, "content")
    private val messageRepository = MessageSQLRepository()
    private lateinit var dockerComposeFile: File

    @JvmStatic
    @BeforeAll
    fun setUpAll() {
        dockerComposeFile = generateDockerComposeFile()
    }

    @BeforeEach
    fun setUp() {
        executeDockerComposeCmd(dockerComposeFile, "up", "--wait")
        setUpDatabase()
    }

    private fun setUpDatabase() {
        listOf(userRepository, friendshipRepository, friendshipRequestRepository, messageRepository).forEach {
            it.connect(host, port, database, user, password)
        }

        // in order to save a message, two users and a friendship between them are needed. Otherwise, an exception will be thrown.
        userRepository.save(userTo)
        userRepository.save(userFrom)
        friendshipRequestRepository.save(FriendshipRequest.of(userTo, userFrom))
        friendshipRepository.save(friendship)
    }

    @AfterEach
    fun tearDown() {
        executeDockerComposeCmd(dockerComposeFile, "down", "-v")
    }

    @Timeout(5 * 60)
    @Test
    fun save() {
        messageRepository.save(message)
        messageRepository.findById(message.id)?.let {
            assertEquals(message, it)
        }
    }

    @Timeout(5 * 60)
    @Test
    fun doesNotSaveDoubles() {
        messageRepository.save(message)
        assertThrows<SQLIntegrityConstraintViolationException> {
            messageRepository.save(message)
        }
    }

    @Timeout(5 * 60)
    @Test
    fun doesNotSaveIfSameID() {
        messageRepository.save(message)
        assertThrows<SQLIntegrityConstraintViolationException> {
            messageRepository.save(Message.of(message.id.value, message.friendship, "otherContent"))
        }
    }

    @Timeout(5 * 60)
    @Test
    fun deleteById() {
        messageRepository.save(message)
        messageRepository.deleteById(message.id)?.let {
            assertEquals(message, it)
        }
    }

    @Timeout(5 * 60)
    @Test
    fun deleteByIdReturnsNullIfNotFound() {
        messageRepository.deleteById(message.id)?.let {
            assertEquals(null, it)
        }
    }

    @Timeout(5 * 60)
    @Test
    fun findAll() {
        messageRepository.save(message)
        messageRepository.save(message2)
        val users = messageRepository.findAll().toList()
        assertAll(
            { assertTrue(users.size == 2) },
            { assertTrue(users.contains(message)) },
            { assertTrue(users.contains(message2)) },
        )
    }
}
