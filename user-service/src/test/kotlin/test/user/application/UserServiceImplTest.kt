package test.user.application

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import social.common.events.UserCreated
import social.common.events.UserUpdated
import social.user.application.UserRepository
import social.user.application.UserServiceImpl
import social.user.domain.User
import social.user.domain.UserID
import social.user.infrastructure.controller.event.KafkaUserProducerVerticle
import social.user.infrastructure.persitence.sql.UserSQLRepository
import java.lang.reflect.Field

class UserServiceImplTest {
    private val repository: UserRepository = mock<UserSQLRepository>()
    private val mockKafkaProducer: KafkaUserProducerVerticle = mock()
    private lateinit var service: UserServiceImpl
    private val user = User.of("test.email76@gmail.com", "username")
    private val nonExistingUserID = UserID.of("nonExistingUserID")

    init {
        `when`(repository.findById(user.id)).thenReturn(user)
        `when`(repository.findById(nonExistingUserID)).thenReturn(null)
        doNothing().`when`(mockKafkaProducer).publishEvent(UserCreated(user.id.value, user.username))
        doNothing().`when`(mockKafkaProducer).publishEvent(UserUpdated(user.id.value, user.username))
    }

    @BeforeEach
    fun setUp() {
        service = UserServiceImpl(repository, mockKafkaProducer)
        val kafkaProducerField: Field = UserServiceImpl::class.java.getDeclaredField("kafkaProducer")
        kafkaProducerField.isAccessible = true
        kafkaProducerField.set(service, mockKafkaProducer)
    }

    @Test
    fun addUser() {
        assertDoesNotThrow { service.addUser(user) }
    }

    @Test
    fun getUser() {
        val actual = service.getUser(user.id)
        assertEquals(user, actual)
    }

    @Test
    fun getNonExistingUser() {
        val actual = service.getUser(nonExistingUserID)
        assertEquals(null, actual)
    }
}
