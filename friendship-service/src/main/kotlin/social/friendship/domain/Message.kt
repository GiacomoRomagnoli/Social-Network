package social.friendship.domain

import social.common.ddd.AggregateRoot
import social.common.ddd.Factory
import social.common.ddd.ID
import social.friendship.domain.Message.MessageID
import java.time.Instant
import java.util.UUID

/**
 * Class to represent a message.
 */
class Message private constructor(
    uuid: UUID,
    val sender: User,
    val receiver: User,
    val content: String,
    val timestamp: Instant
) : AggregateRoot<MessageID>(MessageID(uuid)) {

    /**
     * Data class to represent the message ID.
     */
    class MessageID(value: UUID) : ID<UUID>(value)

    /**
     * Factory to create a message.
     */
    companion object : Factory<Message> {
        /**
         * Creates a message.
         * @param sender the sender of the message
         * @param receiver the receiver of the message
         * @param content the content of the message
         * @return the message with a random uuid and timestamp now
         */
        fun of(sender: User, receiver: User, content: String) =
            of(UUID.randomUUID(), sender, receiver, content, Instant.now())

        /**
         * Creates a message.
         * @param uuid the UUID that identify the massage
         * @param sender the sender of the message
         * @param receiver the receiver of the message
         * @param content the content of the message
         * @return the message with timestamp now
         */
        fun of(uuid: UUID, sender: User, receiver: User, content: String) =
            of(uuid, sender, receiver, content, Instant.now())

        /**
         * Creates a message.
         * @param uuid the UUID that identify the massage
         * @param sender the sender of the message
         * @param receiver the receiver of the message
         * @param content the content of the message
         * @param timestamp the timestamp when the massage has been created
         * @return the message
         */
        fun of(uuid: UUID, sender: User, receiver: User, content: String, timestamp: Instant) =
            validate(Message(uuid, sender, receiver, content, timestamp))

        /**
         * Checks if the message is valid.
         * @param msg the massage to be validated
         * @throws IllegalArgumentException if the message content is blank or the sender is the same as the receiver
         * @return the valid message for fluency
         */
        private fun validate(msg: Message): Message {
            if (msg.sender == msg.receiver) {
                throw IllegalArgumentException("User cannot send a message to itself")
            }
            if (msg.content.isBlank()) {
                throw IllegalArgumentException("Message content cannot be blank")
            }
            return msg
        }
    }
}
