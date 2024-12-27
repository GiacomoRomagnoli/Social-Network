package social.friendship.social.friendship.domain

import social.common.ddd.AggregateRoot
import social.common.ddd.ID
import java.util.UUID

class Message private constructor(val friendship: Friendship, val content: String) : AggregateRoot<Message.MessageID>(MessageID(UUID.randomUUID())) {
    /**
     * Data class to represent the message ID.
     */
    data class MessageID(val value: UUID) : ID<UUID>(value)

    companion object {
        fun of(friendship: Friendship, content: String): Message = Message(friendship, content)
    }
}
