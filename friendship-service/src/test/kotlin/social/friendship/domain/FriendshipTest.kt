package social.friendship.domain

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import social.friendship.social.friendship.domain.User

class FriendshipTest {
    private val from = User.of("user1")
    private val to = User.of("user2")

    @Test
    fun friendshipCreation() {
        val friendship = Friendship.of(to, from)

        assertAll(
            { assertEquals(from, friendship.from) },
            { assertEquals(to, friendship.to) }
        )
    }

    @Test
    fun friendshipCreationUsingFriendshipRequest() {
        val friendshipRequest = FriendshipRequest.of(to, from)
        val friendship = Friendship.of(friendshipRequest)

        assertAll(
            { assertEquals(from, friendship.from) },
            { assertEquals(to, friendship.to) }
        )
    }

    @Test
    fun testEqualsIfSameIdentifier() {
        val friendship1 = Friendship.of(to, from)
        val friendship2 = Friendship.of(to, from)

        assertTrue(friendship1 == friendship2)
    }

    @Test
    fun testDifferentEqualsIfDifferentIdentifier() {
        val friendship1a = Friendship.of(from, to)
        val friendship1b = Friendship.of(to, from)

        val friendship2a = Friendship.of(from, to)
        val friendship2b = Friendship.of(from, from)

        val friendship3a = Friendship.of(from, to)
        val friendship3b = Friendship.of(to, to)

        assertAll(
            { assertNotEquals(friendship1a, friendship1b) },
            { assertNotEquals(friendship2a, friendship2b) },
            { assertNotEquals(friendship3a, friendship3b) }
        )
    }

    @Test
    fun testHashcode() {
        val friendship1 = Friendship.of(to, from)
        val friendship2 = Friendship.of(to, from)

        assertEquals(friendship1.hashCode(), friendship2.hashCode())
    }

    @Test
    fun cannotCreateFriendshipWithSameUser() {
        assertThrows<IllegalArgumentException> {
            Friendship.of(from, from)
        }
    }
}
