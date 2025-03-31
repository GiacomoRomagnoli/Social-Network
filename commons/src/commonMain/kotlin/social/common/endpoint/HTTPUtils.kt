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
    const val FRIENDSHIP_MESSAGES_PATH = "$FRIENDSHIP_FRIENDS_PATH/messages"
    private const val CONTENT_PATH = "/contents"

    const val CREDENTIALS = "$USER_USERS_PATH/credentials"
    const val EMAIL_PARAM = "email"
    const val USER_EMAIL_PARAM = "$USER_USERS_PATH/:$EMAIL_PARAM"
    const val HEALTH = "/health"
    const val USER = USER_USERS_PATH
    const val LOGIN = "/login"
    const val FRIENDSHIP = "$FRIENDSHIP_FRIENDS_PATH/friendships"
    const val FRIENDSHIP_EMAIL_PARAM = "$FRIENDSHIP/:$EMAIL_PARAM"
    const val FRIENDSHIP_REQUEST = FRIENDSHIP_REQUESTS_PATH
    const val FRIENDSHIP_REQUEST_EMAIL_PARAM = "$FRIENDSHIP_REQUEST/:$EMAIL_PARAM"
    const val FRIENDSHIP_REQUEST_SEND = "$FRIENDSHIP_REQUESTS_PATH/send"
    const val FRIENDSHIP_REQUEST_ACCEPT = "$FRIENDSHIP_REQUESTS_PATH/accept"
    const val FRIENDSHIP_REQUEST_DECLINE = "$FRIENDSHIP_REQUESTS_PATH/decline"
    const val MESSAGE_SEND = "$FRIENDSHIP_MESSAGES_PATH/send"
    const val MESSAGE_RECEIVE = FRIENDSHIP_MESSAGES_PATH
    const val MESSAGE_CHAT = "$FRIENDSHIP_MESSAGES_PATH/chat"
    const val USER_PARAM_1 = "user1"
    const val USER_PARAM_2 = "user2"
    const val MESSAGE_CHAT_PARAMS = "$MESSAGE_CHAT/:$USER_PARAM_1/:$USER_PARAM_2"
    const val UUID = "uuid"
    const val MESSAGE = "$FRIENDSHIP_MESSAGES_PATH/:$UUID"
    const val POST = "$CONTENT_PATH/posts"
    const val FEED = "$POST/feed/:$EMAIL_PARAM"
    const val POST_EMAIL_PARAM = "$POST/:$EMAIL_PARAM"
}

/**
 * Object to represent the status codes used to respond to requests.
 */
@JsExport
object StatusCode {
    const val OK = 200
    const val NO_CONTENT = 204
    const val CREATED = 201
    const val BAD_REQUEST = 400
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
    const val INTERNAL_SERVER_ERROR = 500
    const val SERVICE_UNAVAILABLE = 503
}

/**
 * Object to represent the ports used by the application.
 */
@JsExport
object Port {
    const val HTTP = 8080
}
