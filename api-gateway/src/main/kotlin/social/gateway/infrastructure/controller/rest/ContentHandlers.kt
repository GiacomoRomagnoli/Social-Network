package social.gateway.infrastructure.controller.rest

import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.WebClient
import social.common.endpoint.Endpoint
import social.common.endpoint.Endpoint.EMAIL_PARAM
import social.common.endpoint.Port
import social.gateway.infrastructure.controller.rest.Utils.forwardResponse
import social.gateway.infrastructure.controller.rest.Utils.sendUnauthorizedResponse

object ContentHandlers {
    private val CONTENT_SERVICE = System.getenv("CONTENT_SERVICE_URL") ?: "content-service"

    fun publishPost(context: RoutingContext, webClient: WebClient) {
        val body = context.body().asJsonObject()
        val email = body.getJsonObject("user").getString("email")
        if (email == context.get(AuthHandlers.USER_ID)) {
            webClient
                .post(Port.HTTP, CONTENT_SERVICE, Endpoint.POST)
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(body)
                .onComplete(forwardResponse(context))
            return
        }
        sendUnauthorizedResponse(context)
    }

    fun getFeed(context: RoutingContext, webClient: WebClient) {
        val email = context.pathParam(EMAIL_PARAM)
        if (email == context.get(AuthHandlers.USER_ID)) {
            webClient
                .get(Port.HTTP, CONTENT_SERVICE, "${Endpoint.POST}/feed/$email")
                .apply {
                    val param = context.request().getParam("keyword")
                    if (param != null) addQueryParam("keyword", param)
                }
                .send()
                .onComplete(forwardResponse(context))
            return
        }
        sendUnauthorizedResponse(context)
    }

    fun getPosts(context: RoutingContext, webClient: WebClient) {
        val email = context.pathParam(EMAIL_PARAM)
        if (email == context.get(AuthHandlers.USER_ID)) {
            webClient
                .get(Port.HTTP, CONTENT_SERVICE, "${Endpoint.POST}/$email")
                .send()
                .onComplete(forwardResponse(context))
            return
        }
        sendUnauthorizedResponse(context)
    }
}
