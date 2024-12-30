package social.friendship.domain

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import social.friendship.social.friendship.domain.User

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

        val friendshipRequest2a = FriendshipRequest.of(from, to)
        val friendshipRequest2b = FriendshipRequest.of(from, from)

        val friendshipRequest3a = FriendshipRequest.of(from, to)
        val friendshipRequest3b = FriendshipRequest.of(to, to)

        assertAll(
            { assertNotEquals(friendshipRequest1a, friendshipRequest1b) },
            { assertNotEquals(friendshipRequest2a, friendshipRequest2b) },
            { assertNotEquals(friendshipRequest3a, friendshipRequest3b) }
        )
    }

    @Test
    fun testHashcode() {
        val friendshipRequest1 = FriendshipRequest.of(to, from)
        val friendshipRequest2 = FriendshipRequest.of(to, from)

        assertEquals(friendshipRequest1.hashCode(), friendshipRequest2.hashCode())
    }
}
