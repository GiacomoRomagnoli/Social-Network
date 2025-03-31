package social.gateway.infrastructure.controller.rest

import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.net.JksOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.handler.BodyHandler
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import social.common.endpoint.Endpoint
import social.common.endpoint.Port
import social.common.endpoint.StatusCode

open class GatewayVerticle : AbstractVerticle() {
    private val logger: Logger = LogManager.getLogger(this::class)

    override fun start() {
        val router = Router.router(vertx)
        val webClient = WebClient.create(vertx)
        router.route().handler(BodyHandler.create())
        addEndPoints(router, webClient)
        vertx.createHttpServer(options()).requestHandler(router).listen(Port.HTTP).onComplete {
            if (it.succeeded()) {
                logger.trace("server started")
            } else {
                logger.error(it.cause())
            }
        }
    }

    protected open fun addEndPoints(router: Router, webClient: WebClient) {
        router.get(Endpoint.HEALTH).handler { ctx ->
            logger.trace("GET /health received")
            ctx.response().setStatusCode(StatusCode.OK).end()
        }
        // User
        router.post(Endpoint.LOGIN)
            .handler(handlerOf(UserHandlers::login, webClient))
        router.post(Endpoint.USER)
            .handler(handlerOf(UserHandlers::postUser, webClient))
        router.get(Endpoint.USER_EMAIL_PARAM)
            .handler(AuthHandlers::jwtAuth)
            .handler(handlerOf(UserHandlers::getUser, webClient))
        router.put(Endpoint.USER)
            .handler(AuthHandlers::jwtAuth)
            .handler(handlerOf(UserHandlers::putUser, webClient))
        // Friendship
        router.get(Endpoint.FRIENDSHIP_EMAIL_PARAM)
            .handler(AuthHandlers::jwtAuth)
            .handler(handlerOf(FriendshipHandlers::getFriendships, webClient))
        router.get(Endpoint.FRIENDSHIP_REQUEST_EMAIL_PARAM)
            .handler(AuthHandlers::jwtAuth)
            .handler(handlerOf(FriendshipHandlers::getFriendshipRequests, webClient))
        router.post(Endpoint.FRIENDSHIP_REQUEST_SEND)
            .handler(AuthHandlers::jwtAuth)
            .handler(handlerOf(FriendshipHandlers::postFriendshipRequest, webClient))
        router.put(Endpoint.FRIENDSHIP_REQUEST_ACCEPT)
            .handler(AuthHandlers::jwtAuth)
            .handler(handlerOf(FriendshipHandlers::acceptFriendshipRequest, webClient))
        router.put(Endpoint.FRIENDSHIP_REQUEST_DECLINE)
            .handler(AuthHandlers::jwtAuth)
            .handler(handlerOf(FriendshipHandlers::declineFriendshipRequest, webClient))
        // Messages
        router.post(Endpoint.MESSAGE_SEND)
            .handler(AuthHandlers::jwtAuth)
            .handler(handlerOf(MessageHandlers::sendMessage, webClient))
        router.get(Endpoint.MESSAGE_CHAT_PARAMS)
            .handler(AuthHandlers::jwtAuth)
            .handler(handlerOf(MessageHandlers::getChat, webClient))
        router.get(Endpoint.MESSAGE)
            .handler(AuthHandlers::jwtAuth)
            .handler(handlerOf(MessageHandlers::getMessage, webClient))
        // Contents
        router.post(Endpoint.POST)
            .handler(AuthHandlers::jwtAuth)
            .handler(handlerOf(ContentHandlers::publishPost, webClient))
        router.get(Endpoint.FEED)
            .handler(AuthHandlers::jwtAuth)
            .handler(handlerOf(ContentHandlers::getFeed, webClient))
        router.get(Endpoint.POST_EMAIL_PARAM)
            .handler(AuthHandlers::jwtAuth)
            .handler(handlerOf(ContentHandlers::getPosts, webClient))
    }

    protected open fun options(): HttpServerOptions {
        val keystoreStream = javaClass.classLoader.getResourceAsStream("keystore.jks")
            ?: throw IllegalArgumentException("Keystore not found in classpath")
        return HttpServerOptions()
            .setSsl(true)
            .setKeyStoreOptions(
                JksOptions()
                    .setValue(Buffer.buffer(keystoreStream.readBytes()))
                    .setPassword("secret")
            )
    }

    protected fun handlerOf(f: (RoutingContext, WebClient) -> Unit, webClient: WebClient): (RoutingContext) -> Unit =
        {
            ctx ->
            f(ctx, webClient)
        }
}
