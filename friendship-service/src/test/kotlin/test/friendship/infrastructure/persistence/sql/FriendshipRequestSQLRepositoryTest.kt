package test.friendship.infrastructure.persistence.sql

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import social.friendship.domain.FriendshipRequest
import social.friendship.domain.User
import social.friendship.infrastructure.persistence.sql.FriendshipRequestSQLRepository
import social.friendship.infrastructure.persistence.sql.UserSQLRepository
import test.friendship.infrastructure.DockerSQLTest
import java.io.File
import java.sql.SQLIntegrityConstraintViolationException

class FriendshipRequestSQLRepositoryTest : DockerSQLTest() {
    private val userTo = User.of("userToID")
    private val userTo2 = User.of("userToID2")
    private val userFrom = User.of("userFromID")
    private val userFrom2 = User.of("userFromID2")
    private val userRepository = UserSQLRepository()
    private val friendshipRequest = FriendshipRequest.of(userTo, userFrom)
    private val friendshipRequest2 = FriendshipRequest.of(userTo2, userFrom2)
    private val friendshipRequestRepository = FriendshipRequestSQLRepository()
    private val dockerComposePath = "/social/friendship/infrastructure/persistence/sql/docker-compose.yml"
    private lateinit var dockerComposeFile: File

    @BeforeEach
    fun setUp() {
        val dockerComposeResource = this::class.java.getResource(dockerComposePath) ?: throw Exception("Resource not found")
        dockerComposeFile = File(dockerComposeResource.toURI())

        executeDockerComposeCmd(dockerComposeFile, "up", "--wait")
        setUpDatabase()
    }

    private fun setUpDatabase() {
        listOf(userRepository, friendshipRequestRepository).forEach {
            it.connect(localhostIP, port, database, user, password)
        }

        // in order to store a friendship request, two users are needed. Otherwise, an exception will be thrown.
        userRepository.save(userTo)
        userRepository.save(userFrom)
        userRepository.save(userTo2)
        userRepository.save(userFrom2)
    }

    @AfterEach
    fun tearDown() {
        // stops and removes the container, also removes the volumes in order to start fresh each time
        executeDockerComposeCmd(dockerComposeFile, "down", "-v")
    }

    @Timeout(5 * 60)
    @Test
    fun save() {
        friendshipRequestRepository.save(friendshipRequest)
        val actual = friendshipRequestRepository.findById(friendshipRequest.id)
        assertEquals(friendshipRequest, actual)
    }

    @Timeout(5 * 60)
    @Test
    fun doesNotSaveDoubles() {
        friendshipRequestRepository.save(friendshipRequest)
        assertThrows<SQLIntegrityConstraintViolationException> {
            friendshipRequestRepository.save(friendshipRequest)
        }
    }

    @Timeout(5 * 60)
    @Test
    fun doesNotSaveIfSameID() {
        friendshipRequestRepository.save(friendshipRequest)
        assertThrows<SQLIntegrityConstraintViolationException> {
            friendshipRequestRepository.save(FriendshipRequest.of(userTo, userFrom))
        }
    }

    @Timeout(5 * 60)
    @Test
    fun deleteById() {
        friendshipRequestRepository.save(friendshipRequest)
        val actual = friendshipRequestRepository.deleteById(friendshipRequest.id)
        assertEquals(friendshipRequest, actual)
    }

    @Timeout(5 * 60)
    @Test
    fun deleteByIdReturnsNullIfNotFound() {
        val actual = friendshipRequestRepository.deleteById(friendshipRequest.id)
        assertEquals(null, actual)
    }

    @Timeout(5 * 60)
    @Test
    fun findAll() {
        friendshipRequestRepository.save(friendshipRequest)
        friendshipRequestRepository.save(friendshipRequest2)
        val users = friendshipRequestRepository.findAll().toList()
        assertAll(
            { assertTrue(users.size == 2) },
            { assertTrue(users.contains(friendshipRequest)) },
            { assertTrue(users.contains(friendshipRequest2)) },
        )
    }

    @Timeout(5 * 60)
    @Test
    fun deleteFriendshipRequestIfUserToIsDeleted() {
        friendshipRequestRepository.save(friendshipRequest)
        val before = friendshipRequestRepository.findById(friendshipRequest.id)
        userRepository.deleteById(userTo.id)
        val after = friendshipRequestRepository.findById(friendshipRequest.id)
        assertAll(
            { assertEquals(friendshipRequest, before) },
            { assertEquals(null, after) }
        )
    }

    @Timeout(5 * 60)
    @Test
    fun deleteFriendshipRequestIfUserFromIsDeleted() {
        friendshipRequestRepository.save(friendshipRequest)
        val before = friendshipRequestRepository.findById(friendshipRequest.id)
        userRepository.deleteById(userFrom.id)
        val after = friendshipRequestRepository.findById(friendshipRequest.id)
        assertAll(
            { assertEquals(friendshipRequest, before) },
            { assertEquals(null, after) }
        )
    }

    @Timeout(5 * 60)
    @Test
    fun deleteFriendshipRequestIfAtBothUsersAreDeleted() {
        friendshipRequestRepository.save(friendshipRequest)
        val before = friendshipRequestRepository.findById(friendshipRequest.id)
        userRepository.deleteById(userTo.id)
        userRepository.deleteById(userFrom.id)
        val after = friendshipRequestRepository.findById(friendshipRequest.id)
        assertAll(
            { assertEquals(friendshipRequest, before) },
            { assertEquals(null, after) }
        )
    }

    @Timeout(5 * 60)
    @Test
    fun getAllFriendshipRequestsOfUser() {
        val friendshipRequest1 = FriendshipRequest.of(userTo, userFrom)
        val friendshipRequest2 = FriendshipRequest.of(userTo, userFrom2)
        friendshipRequestRepository.save(friendshipRequest1)
        friendshipRequestRepository.save(friendshipRequest2)
        val actualUserTo = friendshipRequestRepository.getAllFriendshipRequestsOf(userTo.id).toList()
        val actualUserFrom = friendshipRequestRepository.getAllFriendshipRequestsOf(userFrom.id).toList()
        val actualUserFrom2 = friendshipRequestRepository.getAllFriendshipRequestsOf(userFrom2.id).toList()

        assertAll(
            { assertTrue(actualUserTo.size == 2) },
            { assertTrue(actualUserTo.containsAll(listOf(friendshipRequest1, friendshipRequest2))) },
            { assertTrue(actualUserFrom.size == 1) },
            { assertTrue(actualUserFrom.contains(friendshipRequest1)) },
            { assertTrue(actualUserFrom2.size == 1) },
            { assertTrue(actualUserFrom2.contains(friendshipRequest2)) }
        )
    }

    @Timeout(5 * 60)
    @Test
    fun getAllFriendshipRequestsOfUserReturnsEmptyListIfNoRequests() {
        val actual = friendshipRequestRepository.getAllFriendshipRequestsOf(userTo.id).toList()
        assertTrue(actual.isEmpty())
    }
}
