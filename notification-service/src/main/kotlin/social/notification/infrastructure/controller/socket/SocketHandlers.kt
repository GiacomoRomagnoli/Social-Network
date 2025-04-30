package social.notification.infrastructure.controller.socket

import io.jsonwebtoken.Jwts
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonObject
import org.apache.logging.log4j.LogManager
import social.common.ddd.DomainEvent
import social.notification.infrastructure.serialization.jackson.Mapper
import java.security.PublicKey
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

object SocketHandlers {
    private val logger = LogManager.getLogger(this::class.java)
    val publicKey = AtomicReference<PublicKey?>(null)
    private val connections = ConcurrentHashMap<String, ServerWebSocket>()

    fun socketHandler(ws: ServerWebSocket) {
        ws.frameHandler {
            if (it.isText) {
                handleAuthentication(ws, it.textData())
            }
        }
        ws.closeHandler {
            logger.info("Socket closed")
            connections.forEach {
                if (it.value == ws) {
                    connections.remove(it.key)
                    logger.info("connection removed")
                }
            }
        }
    }

    fun send(msg: DomainEvent, to: String) {
        logger.info("Sending message: ${Mapper.writeValueAsString(msg)}")
        if (connections[to] == null) logger.info("connection mapping is null")
        connections[to]?.writeTextMessage(Mapper.writeValueAsString(msg))
    }

    private fun handleAuthentication(ws: ServerWebSocket, token: String) {
        try {
            val claims = Jwts.parser().verifyWith(publicKey.get()).build().parseSignedClaims(token).payload
            if (claims["state"] as String == "blocked") {
                ws.close()
                return
            }
            val userId = claims.subject
            connections[userId] = ws
            ws.writeTextMessage(
                JsonObject()
                    .put("type", "authenticated")
                    .put("userId", userId)
                    .encode()
            )
        } catch (e: Exception) {
            println("Authentication failed: ${e.message}")
            ws.close()
        }
    }
}
