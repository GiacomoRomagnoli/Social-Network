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
import social.friendship.domain.User
import social.friendship.infrastructure.DockerSQLTest
import java.io.File
import java.sql.SQLIntegrityConstraintViolationException

object FriendshipSQLRepositoryTest : DockerSQLTest() {
    private val userTo = User.of("userToID")
    private val userTo2 = User.of("userToID2")
    private val userFrom = User.of("userFromID")
    private val userFrom2 = User.of("userFromID2")
    private val userRepository = UserSQLRepository()
    private val friendshipRequestRepository = FriendshipRequestSQLRepository()
    private val friendship = Friendship.of(userTo, userFrom)
    private val friendship2 = Friendship.of(userTo2, userFrom2)
    private val friendshipRepository = FriendshipSQLRepository()
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
        listOf(userRepository, friendshipRequestRepository, friendshipRepository).forEach {
            it.connect(host, port, database, user, password)
        }

        // in order to store a friendship, two users are needed. Otherwise, an exception will be thrown.
        userRepository.save(userTo)
        userRepository.save(userFrom)
        userRepository.save(userTo2)
        userRepository.save(userFrom2)
        friendshipRequestRepository.save(FriendshipRequest.of(userTo, userFrom))
        friendshipRequestRepository.save(FriendshipRequest.of(userTo2, userFrom2))
    }

    @AfterEach
    fun tearDown() {
        executeDockerComposeCmd(dockerComposeFile, "down", "-v")
    }

    @Timeout(5 * 60)
    @Test
    fun save() {
        friendshipRepository.save(friendship)
        val actual = friendshipRepository.findById(friendship.id)
        assertEquals(friendship, actual)
    }

    @Timeout(5 * 60)
    @Test
    fun doesNotSaveDoubles() {
        friendshipRepository.save(friendship)
        assertThrows<SQLIntegrityConstraintViolationException> {
            friendshipRepository.save(friendship)
        }
    }

    @Timeout(5 * 60)
    @Test
    fun doesNotSaveIfSameID() {
        friendshipRepository.save(friendship)
        assertThrows<SQLIntegrityConstraintViolationException> {
            friendshipRepository.save(Friendship.of(userTo, userFrom))
        }
    }

    @Timeout(5 * 60)
    @Test
    fun deleteById() {
        friendshipRepository.save(friendship)
        val actual = friendshipRepository.deleteById(friendship.id)
        assertEquals(friendship, actual)
    }

    @Timeout(5 * 60)
    @Test
    fun deleteByIdReturnsNullIfNotFound() {
        val actual = friendshipRepository.deleteById(friendship.id)
        assertEquals(null, actual)
    }

    @Timeout(5 * 60)
    @Test
    fun findAll() {
        friendshipRepository.save(friendship)
        friendshipRepository.save(friendship2)
        val users = friendshipRepository.findAll().toList()
        assertAll(
            { assertTrue(users.size == 2) },
            { assertTrue(users.contains(friendship)) },
            { assertTrue(users.contains(friendship2)) },
        )
    }

    @Timeout(5 * 60)
    @Test
    fun deleteFriendshipIfUserToIsDeleted() {
        friendshipRepository.save(friendship)
        val before = friendshipRepository.findById(friendship.id)
        userRepository.deleteById(userTo.id)
        val after = friendshipRepository.findById(friendship.id)
        assertAll(
            { assertEquals(friendship, before) },
            { assertEquals(null, after) },
        )
    }

    @Timeout(5 * 60)
    @Test
    fun deleteFriendshipIfUserFromIsDeleted() {
        friendshipRepository.save(friendship)
        val before = friendshipRepository.findById(friendship.id)
        userRepository.deleteById(userFrom.id)
        val after = friendshipRepository.findById(friendship.id)
        assertAll(
            { assertEquals(friendship, before) },
            { assertEquals(null, after) },
        )
    }

    @Timeout(5 * 60)
    @Test
    fun deleteFriendshipIfUserToAndUserFromAreDeleted() {
        friendshipRepository.save(friendship)
        val before = friendshipRepository.findById(friendship.id)
        userRepository.deleteById(userTo.id)
        userRepository.deleteById(userFrom.id)
        val after = friendshipRepository.findById(friendship.id)
        assertAll(
            { assertEquals(friendship, before) },
            { assertEquals(null, after) },
        )
    }

    @Timeout(5 * 60)
    @Test
    fun findAllFriendsOfUser() {
        friendshipRepository.save(Friendship.of(userTo, userFrom))
        friendshipRepository.save(Friendship.of(userTo, userFrom2))
        val friendsOfUserTo = friendshipRepository.findAllFriendsOf(userTo.id).toList()
        val friendsOfUserFrom = friendshipRepository.findAllFriendsOf(userFrom.id).toList()
        val friendsOfUserFrom2 = friendshipRepository.findAllFriendsOf(userFrom2.id).toList()
        assertAll(
            { assertTrue(friendsOfUserTo.size == 2) },
            { assertTrue(friendsOfUserTo.containsAll(listOf(userFrom, userFrom2))) },
            { assertTrue(friendsOfUserFrom.size == 1) },
            { assertTrue(friendsOfUserFrom.contains(userTo)) },
            { assertTrue(friendsOfUserFrom2.size == 1) },
            { assertTrue(friendsOfUserFrom2.contains(userTo)) },
        )
    }

    @Timeout(5 * 60)
    @Test
    fun findAllFriendsOfUserReturnsEmptyListIfUserHasNoFriends() {
        val friends = friendshipRepository.findAllFriendsOf(userTo.id).toList()
        assertTrue(friends.isEmpty())
    }
}
