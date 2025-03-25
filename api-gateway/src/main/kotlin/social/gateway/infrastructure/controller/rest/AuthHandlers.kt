package social.gateway.infrastructure.controller.rest

import io.jsonwebtoken.Jwts
import io.vertx.ext.web.RoutingContext
import social.gateway.infrastructure.controller.rest.Utils.sendServiceUnavailableResponse
import social.gateway.infrastructure.controller.rest.Utils.sendUnauthorizedResponse
import java.security.PublicKey
import java.util.concurrent.atomic.AtomicReference

object AuthHandlers {
    val publicKey = AtomicReference<PublicKey?>(null)
    internal const val USER_ID = "userId"

    fun jwtAuth(context: RoutingContext) {
        if (publicKey.get() == null) {
            sendServiceUnavailableResponse(context, "unable to authenticate")
            return
        }
        val authHeader = context.request().getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorizedResponse(context, "missing token or malformed")
            return
        }
        try {
            val token = authHeader.removePrefix("Bearer ").trim()
            val claims = Jwts.parser().verifyWith(publicKey.get()).build().parseSignedClaims(token)
            context.put(USER_ID, claims.payload.subject)
            context.next()
        } catch (e: Exception) {
            sendUnauthorizedResponse(context, "invalid token: ${e.message}")
        }
    }
}
