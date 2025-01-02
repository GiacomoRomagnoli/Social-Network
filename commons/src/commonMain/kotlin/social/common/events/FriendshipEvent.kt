package social.common.events

import social.common.ddd.DomainEvent
import kotlin.js.JsExport

@JsExport
interface FriendshipEvent : DomainEvent

data class FriendshipRemoved(
    val sender: String,
    val receiver: String,
) : FriendshipEvent {
    companion object {
        const val TOPIC = "friendship-removed"
    }
}

@JsExport
data class FriendshipRequestSent(
    val sender: String,
    val receiver: String,
) : FriendshipEvent {
    companion object {
        const val TOPIC = "friendship-request-sent"
    }
}

@JsExport
data class FriendshipRequestAccepted(
    val sender: String,
    val receiver: String,
) : FriendshipEvent {
    companion object {
        const val TOPIC = "friendship-request-accepted"
    }
}

@JsExport
data class FriendshipRequestRejected(
    val sender: String,
    val receiver: String,
) : FriendshipEvent {
    companion object {
        const val TOPIC = "friendship-request-rejected"
    }
}
