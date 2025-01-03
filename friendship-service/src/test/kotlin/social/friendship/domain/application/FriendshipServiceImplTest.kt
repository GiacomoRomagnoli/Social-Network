package social.friendship.domain.application

import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import social.common.ddd.Repository
import social.friendship.domain.Friendship
import social.friendship.domain.Friendship.FriendshipID
import social.friendship.domain.FriendshipRequest
import social.friendship.domain.FriendshipRequest.FriendshipRequestID
import social.friendship.domain.Message
import social.friendship.domain.Message.MessageID
import social.friendship.social.friendship.domain.User
import social.friendship.social.friendship.domain.User.UserID
import social.friendship.social.friendship.domain.application.FriendshipService
import social.friendship.social.friendship.domain.application.FriendshipServiceImpl
import social.friendship.social.friendship.infrastructure.persistence.sql.FriendshipRequestSQLRepository
import social.friendship.social.friendship.infrastructure.persistence.sql.FriendshipSQLRepository
import social.friendship.social.friendship.infrastructure.persistence.sql.MessageSQLRepository
import social.friendship.social.friendship.infrastructure.persistence.sql.UserSQLRepository
import kotlin.jvm.java
import kotlin.test.Test
import kotlin.test.assertEquals

class FriendshipServiceImplTest {
    private val userRepository: Repository<UserID, User> = mock(UserSQLRepository::class.java)
    private val userService: FriendshipService<UserID, User> = FriendshipServiceImpl(userRepository)
    private val user = User.of("id")
    private val nonExistingUser = User.of("nonExistingUserId")

    private val friendshipRepository: Repository<FriendshipID, Friendship> = mock(FriendshipSQLRepository::class.java)
    private val friendshipService: FriendshipService<FriendshipID, Friendship> = FriendshipServiceImpl(friendshipRepository)
    private val userTo = User.of("userToID")
    private val userFrom = User.of("userFromID")
    private val friendship = Friendship.of(userTo, userFrom)
    private val nonExistingUserTo = User.of("nonExistingUserToID")
    private val nonExistingUserFrom = User.of("nonExistingUserFromID")
    private val nonExistingFriendship = Friendship.of(nonExistingUserTo, nonExistingUserFrom)

    private val friendshipRequestRepository: Repository<FriendshipRequestID, FriendshipRequest> = mock(FriendshipRequestSQLRepository::class.java)
    private val friendshipRequestService: FriendshipService<FriendshipRequestID, FriendshipRequest> = FriendshipServiceImpl(friendshipRequestRepository)
    private val friendshipRequest = FriendshipRequest.of(userTo, userFrom)
    private val nonExistingFriendshipRequest = FriendshipRequest.of(nonExistingUserTo, nonExistingUserFrom)

    private val messageRepository: Repository<MessageID, Message> = mock(MessageSQLRepository::class.java)
    private val messageService: FriendshipService<MessageID, Message> = FriendshipServiceImpl(messageRepository)
    private val message = Message.of(friendship, "content")
    private val nonExistingMessage = Message.of(nonExistingFriendship, "content")

    init {
        `when`(userRepository.findById(user.id)).thenReturn(user)
        `when`(friendshipRepository.findById(friendship.id)).thenReturn(friendship)
        `when`(friendshipRequestRepository.findById(friendshipRequest.id)).thenReturn(friendshipRequest)
        `when`(messageRepository.findById(message.id)).thenReturn(message)
    }

    @Test
    fun addUser() {
        assertDoesNotThrow { userService.add(user) }
    }

    @Test
    fun getUser() {
        val actual = userService.getById(user.id)
        assertEquals(user, actual)
    }

    @Test
    fun getNonExistentUser() {
        val actual = userService.getById(nonExistingUser.id)
        assertEquals(null, actual)
    }

    @Test
    fun addFriendship() {
        assertDoesNotThrow { friendshipService.add(friendship) }
    }

    @Test
    fun getFriendship() {
        val actual = friendshipService.getById(friendship.id)
        assertEquals(friendship, actual)
    }

    @Test
    fun getNonExistentFriendship() {
        val actual = friendshipService.getById(nonExistingFriendship.id)
        assertEquals(null, actual)
    }

    @Test
    fun addFriendshipRequest() {
        assertDoesNotThrow { friendshipRequestService.add(friendshipRequest) }
    }

    @Test
    fun getFriendshipRequest() {
        val actual = friendshipRequestService.getById(friendshipRequest.id)
        assertEquals(friendshipRequest, actual)
    }

    @Test
    fun getNonExistentFriendshipRequest() {
        val actual = friendshipRequestService.getById(nonExistingFriendshipRequest.id)
        assertEquals(null, actual)
    }

    @Test
    fun addMessage() {
        assertDoesNotThrow { messageService.add(message) }
    }

    @Test
    fun getMessage() {
        val actual = messageService.getById(message.id)
        assertEquals(message, actual)
    }

    @Test
    fun getNonExistentMessage() {
        val actual = messageService.getById(nonExistingMessage.id)
        assertEquals(null, actual)
    }
}
