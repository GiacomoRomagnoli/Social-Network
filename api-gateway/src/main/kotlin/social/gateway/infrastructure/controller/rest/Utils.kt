package social.gateway.infrastructure.controller.rest

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.HttpResponse
import social.common.endpoint.StatusCode

object Utils {
    fun forwardResponse(context: RoutingContext): (AsyncResult<HttpResponse<Buffer>>) -> Unit =
        {
            if (it.succeeded()) {
                sendResponse(context, it.result().statusCode(), it.result().body())
            } else {
                sendServiceUnavailableResponse(context, it.cause().message)
            }
        }

    fun sendUnauthorizedResponse(context: RoutingContext, msg: String? = null): Future<Void> =
        sendResponse(context, StatusCode.UNAUTHORIZED, msg)

    fun sendServiceUnavailableResponse(context: RoutingContext, msg: String? = null): Future<Void> =
        sendResponse(context, StatusCode.SERVICE_UNAVAILABLE, msg)

    private fun sendResponse(context: RoutingContext, statusCode: Int, msg: String?): Future<Void> =
        context.response()
            .setStatusCode(statusCode)
            .let { if (msg != null) it.end(msg) else it.end() }

    private fun sendResponse(context: RoutingContext, statusCode: Int, msg: Buffer?): Future<Void> =
        context.response()
            .setStatusCode(statusCode)
            .let { if (msg != null) it.end(msg) else it.end() }
}
