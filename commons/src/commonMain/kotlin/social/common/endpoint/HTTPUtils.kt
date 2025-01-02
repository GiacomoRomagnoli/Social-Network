package social.common.endpoint

import kotlin.js.JsExport

@JsExport
object Endpoint {
    const val USER = "/app/user"
    const val FRIENDSHIP = "/app/friendship"
    const val FRIENDSHIP_REQUEST = "/app/friendship-request"
    const val MESSAGE = "/app/message"
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
