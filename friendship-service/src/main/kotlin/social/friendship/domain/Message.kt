package social.friendship.domain

import social.common.ddd.AggregateRoot
import social.common.ddd.Factory
import social.common.ddd.ID
import social.friendship.domain.Message.MessageID
import java.util.UUID

class Message private constructor(messageId: UUID, val friendship: Friendship, val content: String) : AggregateRoot<MessageID>(MessageID(messageId)) {
    /**
     * Data class to represent the message ID.
     */
    data class MessageID(val value: UUID) : ID<UUID>(value)

    companion object : Factory<Message> {
        fun of(friendship: Friendship, content: String): Message = Message(UUID.randomUUID(), friendship, content)

        fun of(messageId: UUID, friendship: Friendship, content: String): Message = Message(messageId, friendship, content)
    }
}
