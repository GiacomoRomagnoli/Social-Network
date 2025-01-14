package social.common.events

import social.common.ddd.DomainEvent
import kotlin.js.JsExport

@JsExport
interface MessageEvent : DomainEvent

@JsExport
data class MessageSent(
    val id: String,
    val sender: String,
    val receiver: String,
    val message: String,
) : MessageEvent {
    companion object {
        const val TOPIC = "message-sent"
    }
}

@JsExport
data class MessageReceived(
    val id: String,
    val sender: String,
    val receiver: String,
    val message: String,
) : MessageEvent {
    companion object {
        const val TOPIC = "message-received"
    }
}
