package test.user.application

import io.jsonwebtoken.Jwts
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import social.user.application.AuthServiceImpl
import social.user.application.CredentialsRepository
import social.user.application.UserRepository
import social.user.domain.Credentials
import social.user.domain.User
import social.user.infrastructure.controller.event.KafkaUserProducerVerticle
import social.user.infrastructure.persitence.sql.CredentialsSQLRepository

class AuthServiceImplTest {
    private val credentialsRepository: CredentialsRepository = mock<CredentialsSQLRepository>()
    private val userRepository = mock<UserRepository>()
    private val mockKafkaProducer: KafkaUserProducerVerticle = mock()
    private val credentials = Credentials.of("email@domain.org", "1ValidPassword!")
    private val user = User.of(credentials.id.value, "name")
    private lateinit var service: AuthServiceImpl

    @BeforeEach
    fun setUp() {
        `when`(credentialsRepository.findById(credentials.id)).thenReturn(credentials)
        `when`(userRepository.findById(credentials.id)).thenReturn(user)
        service = AuthServiceImpl(credentialsRepository, userRepository, mockKafkaProducer)
    }

    @Test
    fun login() {
        val jwt = service.login(Credentials.of(credentials.id.value, "1ValidPassword!"))
        assertDoesNotThrow {
            println(
                Jwts.parser().verifyWith(service.publicKey).build().parse(jwt)
            )
        }
    }
}
