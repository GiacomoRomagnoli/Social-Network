package social.user.application

import io.jsonwebtoken.Jwts
import social.common.ddd.Service
import social.common.events.AuthKeyGenerated
import social.user.domain.Credentials
import java.security.PublicKey
import java.util.Base64
import java.util.Date

interface AuthService : Service {
    fun login(credentials: Credentials): String
    fun addCredentials(credentials: Credentials)
}

class AuthServiceImpl(
    private val credentialsRepository: CredentialsRepository,
    private val userRepository: UserRepository,
    publisher: KafkaProducerVerticle
) : AuthService {
    private val keys = Jwts.SIG.RS256.keyPair().build()
    val publicKey: PublicKey
        get() = keys.public

    init {
        publisher.publishEvent(
            AuthKeyGenerated(
                Base64.getEncoder().encodeToString(keys.public.encoded)
            )
        )
    }

    override fun login(credentials: Credentials): String {
        val dbCredentials = credentialsRepository.findById(credentials.id)
            ?: throw IllegalArgumentException("credentials do not exists")
        if (dbCredentials.password.match(credentials.password)) {
            val user = userRepository.findById(credentials.id)
                ?: throw IllegalArgumentException("user does not exists")
            return Jwts.builder()
                .subject(dbCredentials.id.value)
                .claim("role", if (user.isAdmin) "admin" else "user")
                .claim("state", if (user.isBlocked) "blocked" else "unblocked")
                .expiration(Date(System.currentTimeMillis() + (15 * 60 * 1000)))
                .signWith(keys.private)
                .compact()
        }
        throw IllegalArgumentException("password does not match")
    }

    override fun addCredentials(credentials: Credentials) {
        credentialsRepository.save(credentials)
    }
}
