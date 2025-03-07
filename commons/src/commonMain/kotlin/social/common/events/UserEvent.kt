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
 * Event to represent a user that has been updated.
 */
@JsExport
data class UserUpdated(
    val username: String,
    val email: String,
) : UserEvent {
    companion object {
        const val TOPIC = "user-updated"
    }
}

/**
 * Event to represent a user deletion.
 */
@JsExport
data class UserBlocked(
    val username: String,
) : UserEvent {
    companion object {
        const val TOPIC = "user-blocked"
    }
}

/**
 * Event to represent a user unblock.
 */
@JsExport
data class UserUnblocked(
    val username: String,
) : UserEvent {
    companion object {
        const val TOPIC = "user-unblocked"
    }
}

/**
 * Event to represent a user deletion.
 */
@JsExport
data class UserLoggedOut(
    val username: String,
) : UserEvent {
    companion object {
        const val TOPIC = "user-logged-out"
    }
}

/**
 * Event to represent admin unblock.
 */
@JsExport
data class AdminLoggedOut(
    val username: String,
) : UserEvent {
    companion object {
        const val TOPIC = "admin-logged-out"
    }
}

/**
 * Event to represent a user login.
 */
@JsExport
data class UserLoggedIn(
    val username: String,
) : UserEvent {
    companion object {
        const val TOPIC = "user-logged-in"
    }
}

/**
 * Event to represent admin login.
 */
@JsExport
data class AdminLoggedIn(
    val username: String,
) : UserEvent {
    companion object {
        const val TOPIC = "admin-logged-in"
    }
}

@JsExport
data class AuthKeyGenerated(
    val publicKey: String,
) : UserEvent {
    companion object {
        const val TOPIC = "auth-key-generated"
    }
}
