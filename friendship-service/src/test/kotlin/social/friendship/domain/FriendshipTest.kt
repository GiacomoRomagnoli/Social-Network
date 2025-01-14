package social.friendship.domain

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import social.friendship.social.friendship.domain.User

class FriendshipTest {
    private val user1 = User.of("user1")
    private val user2 = User.of("user2")

    @Test
    fun friendshipCreation() {
        val friendship = Friendship.of(user2, user1)

        assertAll(
            { assertEquals(user1, friendship.user1) },
            { assertEquals(user2, friendship.user2) }
        )
    }

    @Test
    fun friendshipCreationUsingFriendshipRequest() {
        val friendshipRequest = FriendshipRequest.of(user2, user1)
        val friendship = Friendship.of(friendshipRequest)

        assertAll(
            { assertEquals(user1, friendship.user1) },
            { assertEquals(user2, friendship.user2) }
        )
    }

    @Test
    fun testEqualsIfSameIdentifier() {
        val friendship1 = Friendship.of(user2, user1)
        val friendship2 = Friendship.of(user2, user1)

        assertTrue(friendship1 == friendship2)
    }

    @Test
    fun friendshipIDsAreAlwaysOrderedLexicographically() {
        val friendship1 = Friendship.of(user2, user1)
        val friendship2 = Friendship.of(user1, user2)

        assertEquals(friendship1.id, friendship2.id)
    }

    @Test
    fun testHashcode() {
        val friendship1 = Friendship.of(user2, user1)
        val friendship2 = Friendship.of(user2, user1)

        assertEquals(friendship1.hashCode(), friendship2.hashCode())
    }

    @Test
    fun cannotCreateFriendshipWithSameUser() {
        assertThrows<IllegalArgumentException> {
            Friendship.of(from, from)
        }
    }
}
