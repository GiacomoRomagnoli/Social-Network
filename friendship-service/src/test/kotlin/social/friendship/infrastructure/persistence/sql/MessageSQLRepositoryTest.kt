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
import java.util.UUID

object MessageSQLRepositoryTest : DockerSQLTest() {
    private val userTo = User.of("userToID")
    private val userFrom = User.of("userFromID")
    private val userRepository = UserSQLRepository()
    private val friendship = Friendship.of(userTo, userFrom)
    private val friendshipRequestRepository = FriendshipRequestSQLRepository()
    private val friendshipRepository = FriendshipSQLRepository()
    private val message = Message.of(userTo, userFrom, "content")
    private val message2 = Message.of(userTo, userFrom, "content")
    private val messageRepository = MessageSQLRepository()
    private lateinit var dockerComposeFile: File

    @JvmStatic
    @BeforeAll
    fun setUpAll() {
        dockerComposeFile = generateDockerComposeFile("social/friendship/infrastructure/persistence/sql/")
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
        val actual = messageRepository.findById(message.id)
        assertAll(
            { assertTrue(actual != null) },
            { assertTrue(actual == message) }
        )
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
            messageRepository.save(Message.of(message.id.value, message.sender, message.receiver, "otherContent"))
        }
    }

    @Timeout(5 * 60)
    @Test
    fun deleteById() {
        messageRepository.save(message)
        val actual = messageRepository.deleteById(message.id)
        assertAll(
            { assertTrue(actual != null) },
            { assertTrue(actual == message) }
        )
    }

    @Timeout(5 * 60)
    @Test
    fun deleteByIdReturnsNullIfNotFound() {
        val actual = messageRepository.deleteById(message.id)
        assertTrue(actual == null)
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

    @Timeout(5 * 60)
    @Test
    fun updateMessageContent() {
        val id = UUID.randomUUID()
        val original = Message.of(id, userTo, userFrom, "content")
        messageRepository.save(original)
        val updated = Message.of(id, userTo, userFrom, "new content")
        messageRepository.update(updated)
        val actual = messageRepository.findById(original.id)
        assertAll(
            { assertTrue(actual != null) },
            { assertTrue(actual?.messageId == updated.messageId) },
            { assertTrue(actual?.content == updated.content) }
        )
    }

    @Timeout(5 * 60)
    @Test
    fun updateNonExistingMessage() {
        val updated = Message.of(userTo, userFrom, "new content")
        assertThrows<SQLIntegrityConstraintViolationException> {
            messageRepository.update(updated)
        }
    }

    @Timeout(5 * 60)
    @Test
    fun deleteMessageIfUserToIsDeleted() {
        messageRepository.save(message)
        val before = messageRepository.findById(message.id)
        userRepository.deleteById(userTo.id)
        val after = messageRepository.findById(message.id)
        assertAll(
            { assertEquals(message, before) },
            { assertEquals(null, after) }
        )
    }

    @Timeout(5 * 60)
    @Test
    fun deleteMessageIfUserFromIsDeleted() {
        messageRepository.save(message)
        val before = messageRepository.findById(message.id)
        userRepository.deleteById(userFrom.id)
        val after = messageRepository.findById(message.id)
        assertAll(
            { assertEquals(message, before) },
            { assertEquals(null, after) }
        )
    }

    @Timeout(5 * 60)
    @Test
    fun keepTheMessagesIfFriendshipIsDeleted() {
        messageRepository.save(message)
        val before = messageRepository.findById(message.id)
        friendshipRepository.deleteById(friendship.id)
        val after = messageRepository.findById(message.id)
        assertAll(
            { assertEquals(message, before) },
            { assertEquals(message, after) }
        )
    }

    @Timeout(5 * 60)
    @Test
    fun findAllMessagesReceivedByUser() {
        messageRepository.save(message)
        messageRepository.save(message2)
        val messagesReceivedByUserTo = messageRepository.findAllMessagesReceivedBy(userTo.id).toList()
        val messagesReceivedByUserFrom = messageRepository.findAllMessagesReceivedBy(userFrom.id).toList()
        assertAll(
            { assertTrue(messagesReceivedByUserTo.isEmpty()) },
            { assertTrue(messagesReceivedByUserFrom.size == 2) },
            { assertTrue(messagesReceivedByUserFrom.contains(message)) },
            { assertTrue(messagesReceivedByUserFrom.contains(message2)) }
        )
    }

    @Timeout(5 * 60)
    @Test
    fun findAllMessagesReceivedByUserReturnsEmptyListIfNoMessages() {
        val messages = messageRepository.findAllMessagesReceivedBy(userTo.id).toList()
        assertTrue(messages.isEmpty())
    }
}
