package social.friendship.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import social.friendship.social.friendship.domain.Friendship
import social.friendship.social.friendship.domain.Message
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MessageTest {
    @Test
    fun messageCreation() {
        val message = Message.of(Friendship.of("to", "from"), "content")

        assertAll(
            { assertEquals("to", message.friendship.to) },
            { assertEquals("from", message.friendship.from) },
            { assertEquals("content", message.content) }
        )
    }

    @Test
    fun eachMessageHasAUniqueIdentifier() {
        val message1 = Message.of(Friendship.of("to", "from"), "content")
        val message2 = Message.of(Friendship.of("to", "from"), "content")

        assertNotEquals(message1.id, message2.id)
    }
}
