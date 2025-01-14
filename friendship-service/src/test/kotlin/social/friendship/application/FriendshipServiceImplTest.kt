package social.friendship.application

import io.vertx.core.Vertx
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import social.common.events.FriendshipRemoved
import social.friendship.domain.Friendship
import social.friendship.domain.FriendshipRequest
import social.friendship.domain.Message
import social.friendship.domain.User
import social.friendship.infrastructure.persistence.sql.FriendshipRequestSQLRepository
import social.friendship.infrastructure.persistence.sql.FriendshipSQLRepository
import social.friendship.infrastructure.persistence.sql.MessageSQLRepository
import social.friendship.infrastructure.persistence.sql.UserSQLRepository
import kotlin.test.assertEquals

class FriendshipServiceImplTest {
    private val user = User.of("id")
    private val nonExistingUser = User.of("nonExistingUserId")
    private val userTo = User.of("userToID")
    private val userFrom = User.of("userFromID")
    private val friendship = Friendship.of(userTo, userFrom)
    private val nonExistingUserTo = User.of("nonExistingUserToID")
    private val nonExistingUserFrom = User.of("nonExistingUserFromID")
    private val nonExistingFriendship = Friendship.of(nonExistingUserTo, nonExistingUserFrom)
    private val friendshipRequest = FriendshipRequest.of(userTo, userFrom)
    private val nonExistingFriendshipRequest = FriendshipRequest.of(nonExistingUserTo, nonExistingUserFrom)
    private val sender = userTo
    private val receiver = userFrom
    private val nonExistingSender = nonExistingUserTo
    private val nonExistingReceiver = nonExistingUserFrom
    private val message = Message.of(sender, receiver, "content")
    private val nonExistingMessage = Message.of(nonExistingSender, nonExistingReceiver, "content")
    private val friendshipRemovedEvent = FriendshipRemoved(sender.id.value, receiver.id.value)
    private lateinit var friendshipService: FriendshipServiceVerticle
    private lateinit var closeable: AutoCloseable
    @Mock
    private lateinit var userRepository: UserSQLRepository
    @Mock
    private lateinit var friendshipRepository: FriendshipSQLRepository
    @Mock
    private lateinit var friendshipRequestRepository: FriendshipRequestSQLRepository
    @Mock
    private lateinit var messageRepository: MessageSQLRepository
    @Mock
    private lateinit var kafkaProducer: KafkaFriendshipProducerVerticle

    @BeforeEach
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        friendshipService = FriendshipServiceVerticle(shouldConnectToDB = false)
        friendshipService.apply {
            this::class.java.getDeclaredField("userRepository").apply {
                isAccessible = true
                set(this@FriendshipServiceImplTest.friendshipService, userRepository)
            }
            this::class.java.getDeclaredField("friendshipRepository").apply {
                isAccessible = true
                set(this@FriendshipServiceImplTest.friendshipService, friendshipRepository)
            }
            this::class.java.getDeclaredField("friendshipRequestRepository").apply {
                isAccessible = true
                set(this@FriendshipServiceImplTest.friendshipService, friendshipRequestRepository)
            }
            this::class.java.getDeclaredField("messageRepository").apply {
                isAccessible = true
                set(this@FriendshipServiceImplTest.friendshipService, messageRepository)
            }
            this::class.java.getDeclaredField("kafkaProducer").apply {
                isAccessible = true
                set(this@FriendshipServiceImplTest.friendshipService, kafkaProducer)
            }
        }

        `when`(friendshipRepository.findById(friendship.id)).thenReturn(friendship)
        `when`(friendshipRepository.deleteById(friendship.id)).thenReturn(friendship)
        `when`(friendshipRepository.findAllFriendsOf(user.id)).thenReturn(listOf(user))

        `when`(friendshipRequestRepository.findById(friendshipRequest.id)).thenReturn(friendshipRequest)
        `when`(friendshipRequestRepository.deleteById(friendshipRequest.id)).thenReturn(friendshipRequest)
        `when`(friendshipRequestRepository.getAllFriendshipRequestsOf(user.id)).thenReturn(listOf(friendshipRequest))

        `when`(messageRepository.findById(message.id)).thenReturn(message)
        `when`(messageRepository.findAllMessagesReceivedBy(receiver.id)).thenReturn(listOf(message))
        `when`(messageRepository.findAllMessagesExchangedBetween(sender.id, receiver.id)).thenReturn(listOf(message))

        `when`(userRepository.findById(user.id)).thenReturn(user)

        doNothing().`when`(kafkaProducer).publishEvent(friendshipRemovedEvent)

        val vertx = Vertx.vertx()
        vertx.deployVerticle(friendshipService)
    }

    @AfterEach
    fun tearDown() {
        closeable.close()
    }

    @Test
    fun addUser() {
        assertDoesNotThrow { friendshipService.addUser(user) }
    }

    @Test
    fun getUser() {
        val actual = friendshipService.getUser(user.id)
        assertEquals(user, actual)
    }

    @Test
    fun getNonExistentUser() {
        val actual = friendshipService.getUser(nonExistingUser.id)
        assertEquals(null, actual)
    }

    @Test
    fun addFriendship() {
        assertDoesNotThrow { friendshipService.addFriendship(friendship) }
    }

    @Test
    fun getFriendship() {
        val actual = friendshipService.getFriendship(friendship.id)
        assertEquals(friendship, actual)
    }

    @Test
    fun getNonExistentFriendship() {
        val actual = friendshipService.getFriendship(nonExistingFriendship.id)
        assertEquals(null, actual)
    }

    @Test
    fun deleteFriendship() {
        val actual = friendshipService.deleteFriendship(friendship.id)
        assertEquals(friendship, actual)
    }

    @Test
    fun deleteNonExistingFriendship() {
        val actual = friendshipService.deleteFriendship(nonExistingFriendship.id)
        assertEquals(null, actual)
    }

    @Test
    fun getAllFriendsOfUser() {
        val actual = friendshipService.getAllFriendsByUserId(user.id)
        assertEquals(listOf(user), actual)
    }

    @Test
    fun getAllFriendsOfNonExistingUser() {
        val actual = friendshipService.getAllFriendsByUserId(nonExistingUser.id)
        assertEquals(emptyList(), actual)
    }

    @Test
    fun addFriendshipRequest() {
        assertDoesNotThrow { friendshipService.addFriendshipRequest(friendshipRequest) }
    }

    @Test
    fun getFriendshipRequest() {
        val actual = friendshipService.getFriendshipRequest(friendshipRequest.id)
        assertEquals(friendshipRequest, actual)
    }

    @Test
    fun getNonExistentFriendshipRequest() {
        val actual = friendshipService.getFriendshipRequest(nonExistingFriendshipRequest.id)
        assertEquals(null, actual)
    }

    @Test
    fun rejectFriendshipRequestOfUser() {
        val actual = friendshipService.rejectFriendshipRequest(friendshipRequest)
        assertEquals(friendshipRequest, actual)
    }

    @Test
    fun rejectNonExistentFriendshipRequest() {
        val actual = friendshipService.rejectFriendshipRequest(nonExistingFriendshipRequest)
        assertEquals(null, actual)
    }

    @Test
    fun acceptFriendshipRequest() {
        assertDoesNotThrow { friendshipService.acceptFriendshipRequest(friendshipRequest) }
    }

    @Test
    fun acceptFriendshipRequestOfNonExistingUser() {
        assertThrows<IllegalArgumentException> { friendshipService.acceptFriendshipRequest(nonExistingFriendshipRequest) }
    }

    @Test
    fun getAllFriendshipRequestsByUserId() {
        val actual = friendshipService.getAllFriendshipRequestsByUserId(user.id)
        assertEquals(listOf(friendshipRequest), actual)
    }

    @Test
    fun getAllFriendshipRequestsByNonExistingUserId() {
        val actual = friendshipService.getAllFriendshipRequestsByUserId(nonExistingUser.id)
        assertEquals(emptyList(), actual)
    }

    @Test
    fun addMessage() {
        assertDoesNotThrow { friendshipService.addMessage(message) }
    }

    @Test
    fun getMessage() {
        val actual = friendshipService.getMessage(message.id)
        assertEquals(message, actual)
    }

    @Test
    fun getNonExistentMessage() {
        val actual = friendshipService.getMessage(nonExistingMessage.id)
        assertEquals(null, actual)
    }

    @Test
    fun receivedMessage() {
        assertDoesNotThrow { friendshipService.receivedMessage(message) }
    }

    @Test
    fun sentMessage() {
        assertDoesNotThrow { friendshipService.sentMessage(message) }
    }

    @Test
    fun getAllMessagesReceivedByUserId() {
        val actual = friendshipService.getAllMessagesReceivedByUserId(receiver.id)
        assertEquals(listOf(message), actual)
    }

    @Test
    fun getAllMessagesExchangedBetween() {
        val actual = friendshipService.getAllMessagesExchangedBetween(sender.id, receiver.id)
        assertEquals(listOf(message), actual)
    }
}
