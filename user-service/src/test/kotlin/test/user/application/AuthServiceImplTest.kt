package test.user.application

import io.jsonwebtoken.Jwts
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import social.user.application.AuthServiceImpl
import social.user.application.CredentialsRepository
import social.user.domain.Credentials
import social.user.infrastructure.controller.event.KafkaUserProducerVerticle
import social.user.infrastructure.persitence.sql.CredentialsSQLRepository

class AuthServiceImplTest {
    private val repository: CredentialsRepository = mock<CredentialsSQLRepository>()
    private val mockKafkaProducer: KafkaUserProducerVerticle = mock()
    private val credentials = Credentials.of("email@domain.org", "1ValidPassword!")
    private lateinit var service: AuthServiceImpl

    @BeforeEach
    fun setUp() {
        `when`(repository.findById(credentials.id)).thenReturn(credentials)
        service = AuthServiceImpl(repository, mockKafkaProducer)
    }

    @Test
    fun login() {
        val jwt = service.login(Credentials.of(credentials.id.value, "1ValidPassword!", false))
        assertDoesNotThrow {
            println(
                Jwts.parser().verifyWith(service.publicKey).build().parse(jwt)
            )
        }
    }
}
