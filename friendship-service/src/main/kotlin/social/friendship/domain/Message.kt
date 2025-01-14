package social.friendship.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import social.common.ddd.AggregateRoot
import social.common.ddd.Factory
import social.common.ddd.ID
import social.friendship.domain.Message.MessageID
import social.friendship.social.friendship.domain.User
import java.util.UUID

class Message private constructor(
    @JsonProperty("messageId") val messageId: UUID,
    @JsonProperty("sender") val sender: User,
    @JsonProperty("receiver") val receiver: User,
    @JsonProperty("content") val content: String
) : AggregateRoot<MessageID>(MessageID(messageId)) {

    /**
     * Data class to represent the message ID.
     */
    data class MessageID @JsonCreator constructor(
        @JsonProperty("value") val value: UUID
    ) : ID<UUID>(value)

    companion object : Factory<Message> {
        fun of(friendship: Friendship, content: String): Message = Message(UUID.randomUUID(), friendship, content)

        fun of(messageId: UUID, friendship: Friendship, content: String): Message = Message(messageId, friendship, content)
    }
}
