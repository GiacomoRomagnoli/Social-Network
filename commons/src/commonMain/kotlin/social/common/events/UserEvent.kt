package social.common.events

import social.common.ddd.DomainEvent
import kotlin.js.JsExport

/**
 * Interface to represent a user event.
 */
@JsExport
interface UserEvent : DomainEvent

/**
 * Event to represent a user creation.
 */
@JsExport
data class UserCreated(
    val username: String,
    val email: String,
) : UserEvent {
    companion object {
        const val TOPIC = "user-created"
    }
}

/**
 * Interface to group user events that block or unblock users.
 */
@JsExport
interface BlockingEvent : UserEvent {
    val user: String
    companion object {
        const val TOPIC = "user-blocking-events"
    }
}

/**
 * Event to represent a user deletion.
 */
@JsExport
data class UserBlocked(override val user: String) : BlockingEvent

/**
 * Event to represent a user unblock.
 */
@JsExport
data class UserUnblocked(override val user: String) : BlockingEvent

@JsExport
data class AuthKeyGenerated(
    val publicKey: String,
) : DomainEvent {
    companion object {
        const val TOPIC = "auth-key-generated"
    }
}
