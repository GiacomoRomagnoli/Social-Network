package social.notification.infrastructure.controller.socket

import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.net.JksOptions
import io.vertx.ext.web.Router
import social.common.endpoint.Endpoint
import social.common.endpoint.Port
import social.common.endpoint.StatusCode

class NotificationVerticle : AbstractVerticle() {
    override fun start() {
        val router = Router.router(vertx)
        router.get(Endpoint.HEALTH).handler { it.response().setStatusCode(StatusCode.OK).end() }
        router.get(Endpoint.READY).handler {
            when (SocketHandlers.publicKey.get()) {
                null -> it.response().setStatusCode(StatusCode.SERVICE_UNAVAILABLE).end()
                else -> it.response().setStatusCode(StatusCode.OK).end()
            }
        }
        val server = vertx.createHttpServer(options())
        server.webSocketHandler(SocketHandlers::socketHandler).requestHandler(router).listen(Port.HTTP)
    }

    private fun options(): HttpServerOptions {
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
}
