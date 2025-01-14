package social.friendship.domain

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FriendshipRequestTest {
    private val from = User.of("user1")
    private val to = User.of("user2")

    @Test
    fun friendshipRequestCreation() {
        val friendshipRequest = FriendshipRequest.of(to, from)

        assertAll(
            { assertEquals(from, friendshipRequest.from) },
            { assertEquals(to, friendshipRequest.to) }
        )
    }

    @Test
    fun testEqualsIfSameIdentifier() {
        val friendshipRequest1 = FriendshipRequest.of(to, from)
        val friendshipRequest2 = FriendshipRequest.of(to, from)

        assertTrue(friendshipRequest1 == friendshipRequest2)
    }

    @Test
    fun testDifferentEqualsIfDifferentIdentifier() {
        val friendshipRequest1a = FriendshipRequest.of(from, to)
        val friendshipRequest1b = FriendshipRequest.of(to, from)

        assertNotEquals(friendshipRequest1a, friendshipRequest1b)
    }

    @Test
    fun testHashcode() {
        val friendshipRequest1 = FriendshipRequest.of(to, from)
        val friendshipRequest2 = FriendshipRequest.of(to, from)

        assertEquals(friendshipRequest1.hashCode(), friendshipRequest2.hashCode())
    }

    @Test
    fun cannotCreateFriendshipRequestToItself() {
        assertThrows<IllegalArgumentException> {
            FriendshipRequest.of(from, from)
        }
    }
}
