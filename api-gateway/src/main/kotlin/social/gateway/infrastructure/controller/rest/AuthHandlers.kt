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
    private const val USER_STATE = "userState"
    private const val USER_ROLE = "userRole"

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
            val claims = Jwts.parser().verifyWith(publicKey.get()).build().parseSignedClaims(token).payload
            context.put(USER_ID, claims.subject)
            context.put(USER_ROLE, claims["role"] as String)
            context.put(USER_STATE, claims["state"] as String)
            context.next()
        } catch (e: Exception) {
            sendUnauthorizedResponse(context, "invalid token: ${e.message}")
        }
    }

    fun adminAuth(context: RoutingContext) {
        if ("admin" == context.get(USER_ROLE)) {
            context.next()
            return
        }
        sendUnauthorizedResponse(context, "this operation requires admin authority")
    }

    fun blockedAuth(context: RoutingContext) {
        if ("blocked" != context.get(USER_STATE)) {
            context.next()
            return
        }
        sendUnauthorizedResponse(context, "user unauthorized because is blocked")
    }
}
