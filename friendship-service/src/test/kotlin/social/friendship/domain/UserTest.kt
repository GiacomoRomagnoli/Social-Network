package social.friendship.domain

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import social.friendship.domain.User.UserID

class UserTest {
    private val id = "user"
    private val userID = UserID("user")

    @Test
    fun userCreation() {
        val user = User.of(id)

        assertAll(
            { assertEquals(id, user.id.value) },
            { assertEquals(userID, user.id) }
        )
    }

    @Test
    fun testEqualsIfSameIdentifier() {
        val user1 = User.of(id)
        val user2 = User.of(id)

        assertTrue(user1 == user2)
    }

    @Test
    fun testHashcode() {
        val user1 = User.of(id)
        val user2 = User.of(id)

        assertEquals(user1.hashCode(), user2.hashCode())
    }
}
