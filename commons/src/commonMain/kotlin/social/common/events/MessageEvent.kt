package social.common.events

import social.common.ddd.DomainEvent
import kotlin.js.JsExport

/**
 * Interface to represent a message event.
 */
@JsExport
interface MessageEvent : DomainEvent

/**
 * Data class to represent a message sent event.
 * @param id the ID of the message
 * @param sender the user that sends the message
 * @param receiver the user that receives the message
 * @param message the content of the message
 */
@JsExport
data class MessageSent(
    val id: String,
    val sender: String,
    val receiver: String,
    val message: String,
    val timestamp: String,
) : MessageEvent {
    companion object {
        const val TOPIC = "message-sent"
    }
}

/**
 * Data class to represent a message received event.
 * @param id the ID of the message
 * @param sender the user that sends the message
 * @param receiver the user that receives the message
 * @param message the content of the message
 */
@JsExport
data class MessageReceived(
    val id: String,
    val sender: String,
    val receiver: String,
    val message: String,
    val timestamp: String,
) : MessageEvent {
    companion object {
        const val TOPIC = "message-received"
    }
}
