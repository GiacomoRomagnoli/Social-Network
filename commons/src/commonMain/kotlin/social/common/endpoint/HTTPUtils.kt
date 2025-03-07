package social.common.endpoint

import kotlin.js.JsExport

/**
 * Object to represent the endpoints of the application.
 */
@JsExport
object Endpoint {
    private const val USER_USERS_PATH = "/users"
    private const val FRIENDSHIP_FRIENDS_PATH = "/friends"
    private const val FRIENDSHIP_REQUESTS_PATH = "$FRIENDSHIP_FRIENDS_PATH/requests"
    private const val FRIENDSHIP_MESSAGES_PATH = "$FRIENDSHIP_FRIENDS_PATH/messages"

    const val HEALTH = "/health"
    const val USER = USER_USERS_PATH
    const val LOGIN = "/login"
    const val FRIENDSHIP = "$FRIENDSHIP_FRIENDS_PATH/friendships"
    const val FRIENDSHIP_REQUEST = FRIENDSHIP_REQUESTS_PATH
    const val FRIENDSHIP_REQUEST_SEND = "$FRIENDSHIP_REQUESTS_PATH/send"
    const val FRIENDSHIP_REQUEST_ACCEPT = "$FRIENDSHIP_REQUESTS_PATH/accept"
    const val FRIENDSHIP_REQUEST_DECLINE = "$FRIENDSHIP_REQUESTS_PATH/decline"
    const val MESSAGE_SEND = "$FRIENDSHIP_MESSAGES_PATH/send"
    const val MESSAGE_RECEIVE = FRIENDSHIP_MESSAGES_PATH
    const val MESSAGE_CHAT = "$FRIENDSHIP_MESSAGES_PATH/chat"
}

/**
 * Object to represent the status codes used to respond to requests.
 */
@JsExport
object StatusCode {
    const val OK = 200
    const val CREATED = 201
    const val BAD_REQUEST = 400
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
    const val INTERNAL_SERVER_ERROR = 500
}

/**
 * Object to represent the ports used by the application.
 */
@JsExport
object Port {
    const val HTTP = 8080
}
