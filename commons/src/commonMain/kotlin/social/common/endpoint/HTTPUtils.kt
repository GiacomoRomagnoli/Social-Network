package social.common.endpoint

import kotlin.js.JsExport

@JsExport
object Endpoint {
    private const val FRIENDSHIP_FRIENDS_PATH = "/friends"
    private const val FRIENDSHIP_REQUESTS_PATH = "$FRIENDSHIP_FRIENDS_PATH/requests"
    private const val FRIENDSHIP_MESSAGES_PATH = "$FRIENDSHIP_FRIENDS_PATH/messages"

    const val USER = "/app/user"
    const val FRIENDSHIP = "$FRIENDSHIP_FRIENDS_PATH/friendships"
    const val FRIENDSHIP_REQUEST = FRIENDSHIP_REQUESTS_PATH
    const val FRIENDSHIP_REQUEST_SEND = "$FRIENDSHIP_REQUESTS_PATH/send"
    const val FRIENDSHIP_REQUEST_ACCEPT = "$FRIENDSHIP_REQUESTS_PATH/accept"
    const val FRIENDSHIP_REQUEST_DECLINE = "$FRIENDSHIP_REQUESTS_PATH/decline"
    const val MESSAGE_SEND = "$FRIENDSHIP_MESSAGES_PATH/send"
    const val MESSAGE_RECEIVE = FRIENDSHIP_MESSAGES_PATH
    const val MESSAGE_CHAT = "$FRIENDSHIP_MESSAGES_PATH/chat"
}

@JsExport
object StatusCode {
    const val OK = 200
    const val CREATED = 201
    const val BAD_REQUEST = 400
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
    const val INTERNAL_SERVER_ERROR = 500
}

@JsExport
object Port {
    const val HTTP = 8080
}
