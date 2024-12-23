package social.user.application

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import social.common.ddd.Repository
import social.user.domain.User
import social.user.domain.User.UserID
import social.user.infrastructure.persitence.sql.UserSQLRepository

class UserServiceImplTest {
    private val repository: Repository<UserID, User> = mock(UserSQLRepository::class.java)
    private val service: UserService = UserServiceImpl(repository)
    private val user = User.of("test.email76@gmail.com", "username")
    private val nonExistingUserID = UserID("nonExistingUserID")

    init {
        `when`(repository.findById(user.id)).thenReturn(user)
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

    @Test
    fun updateUser() {
        assertDoesNotThrow { service.updateUser(user) }
    }
}
