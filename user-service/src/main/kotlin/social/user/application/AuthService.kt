package social.user.application

import io.jsonwebtoken.Jwts
import social.common.ddd.Service
import social.common.events.AuthKeyGenerated
import social.user.domain.Credentials
import java.security.PublicKey
import java.util.Base64

interface AuthService : Service {
    fun login(credentials: Credentials): String
    fun addCredentials(credentials: Credentials)
}

class AuthServiceImpl(
    private val repository: CredentialsRepository,
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
        val db = repository.findById(credentials.id) ?: throw IllegalArgumentException("user does not exists")
        if (db.password.match(credentials.password)) {
            return Jwts.builder()
                .subject(db.id.value)
                .signWith(keys.private)
                .compact()
        }
        throw IllegalArgumentException("password does not match")
    }

    override fun addCredentials(credentials: Credentials) {
        repository.save(credentials)
    }
}
