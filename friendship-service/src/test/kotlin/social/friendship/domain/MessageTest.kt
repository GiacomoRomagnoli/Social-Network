package social.friendship.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import social.friendship.social.friendship.domain.User
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MessageTest {
    private val sender = User.of("sender")
    private val receiver = User.of("receiver")

    @Test
    fun messageCreation() {
        val message = Message.of(sender, receiver, "content")

        assertAll(
            { assertEquals(sender, message.sender) },
            { assertEquals(receiver, message.receiver) },
            { assertEquals("content", message.content) }
        )
    }

    @Test
    fun messageCreationWithSetID() {
        val id: UUID = UUID.randomUUID()
        val message = Message.of(id, sender, receiver, "content")

        assertAll(
            { assertEquals(id, message.id.value) },
            { assertEquals(sender, message.sender) },
            { assertEquals(receiver, message.receiver) },
            { assertEquals("content", message.content) }
        )
    }

    @Test
    fun eachMessageHasAUniqueIdentifier() {
        val message1 = Message.of(sender, receiver, "content")
        val message2 = Message.of(sender, receiver, "content")

        assertNotEquals(message1.id, message2.id)
    }

    @Test
    fun testSetIdentifier() {
        val id: UUID = UUID.randomUUID()
        val message1 = Message.of(id, sender, receiver, "content")

        assertEquals(id, message1.id.value)
    }

    @Test
    fun throwsExceptionIfNoContent() {
        assertAll(
            { assertThrows<IllegalArgumentException> { Message.of(sender, receiver, "") } },
            { assertThrows<IllegalArgumentException> { Message.of(UUID.randomUUID(), sender, receiver, "") } }
        )
    }

    @Test
    fun throwsExceptionIfSenderEqualsReceiver() {
        assertAll(
            { assertThrows<IllegalArgumentException> { Message.of(sender, sender, "content") } },
            { assertThrows<IllegalArgumentException> { Message.of(UUID.randomUUID(), sender, sender, "content") } }
        )
    }
}
