package social.friendship.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import social.friendship.social.friendship.domain.User
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MessageTest {
    private val to = User.of("to")
    private val from = User.of("from")

    @Test
    fun messageCreation() {
        val message = Message.of(Friendship.of(to, from), "content")

        assertAll(
            { assertEquals(to, message.friendship.to) },
            { assertEquals(from, message.friendship.from) },
            { assertEquals("content", message.content) }
        )
    }

    @Test
    fun messageCreationWithSetID() {
        val id: UUID = UUID.randomUUID()
        val message = Message.of(id, Friendship.of(to, from), "content")

        assertAll(
            { assertEquals(id, message.id.value) },
            { assertEquals(to, message.friendship.to) },
            { assertEquals(from, message.friendship.from) },
            { assertEquals("content", message.content) }
        )
    }

    @Test
    fun eachMessageHasAUniqueIdentifier() {
        val message1 = Message.of(Friendship.of(to, from), "content")
        val message2 = Message.of(Friendship.of(to, from), "content")

        assertNotEquals(message1.id, message2.id)
    }

    @Test
    fun testSetIdentifier() {
        val id: UUID = UUID.randomUUID()
        val message1 = Message.of(id, Friendship.of(to, from), "content")

        assertEquals(id, message1.id.value)
    }
}
