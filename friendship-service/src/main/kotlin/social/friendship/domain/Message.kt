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
        fun of(sender: User, receiver: User, content: String): Message {
            return createMessage(UUID.randomUUID(), sender, receiver, content)
        }

        fun of(messageId: UUID, sender: User, receiver: User, content: String): Message {
            return createMessage(messageId, sender, receiver, content)
        }

        private fun createMessage(messageId: UUID, sender: User, receiver: User, content: String): Message {
            checkArguments(sender, receiver, content)
            return Message(messageId, sender, receiver, content)
        }

        private fun checkArguments(
            sender: User,
            receiver: User,
            content: String
        ) {
            if (sender == receiver) {
                throw IllegalArgumentException("User cannot send a message to itself")
            }
            if (content.isBlank()) {
                throw IllegalArgumentException("Message content cannot be blank")
            }
        }
    }
}
