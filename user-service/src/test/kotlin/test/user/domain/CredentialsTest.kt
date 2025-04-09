package test.user.domain

import org.junit.jupiter.api.Test
import social.user.domain.Credentials
import social.user.domain.Password
import social.user.domain.UserID
import kotlin.test.assertTrue

class CredentialsTest {

    @Test
    fun testStringCredentialsCreation() {
        val it = Credentials.of("email@domain.org", "ValidPassword123!")
        assertTrue { it.id.value == "email@domain.org" }
        assertTrue { it.password.match("ValidPassword123!") }
    }

    @Test
    fun testObjectsCredentialsCreation() {
        val it = Credentials.of(UserID.of("email@domain.org"), Password.of("ValidPassword123!"))
        assertTrue { it.id.value == "email@domain.org" }
        assertTrue { it.password.match("ValidPassword123!") }
    }

    @Test
    fun testUpdatePassword() {
        val it = Credentials.of("email@domain.org", "ValidPassword123!")
            .updatePassword("AnotherValidPassword123!")
        assertTrue { it.id.value == "email@domain.org" }
        assertTrue { it.password.match("AnotherValidPassword123!") }
    }
}
