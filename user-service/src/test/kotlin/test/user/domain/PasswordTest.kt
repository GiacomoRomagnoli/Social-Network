package test.user.domain

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import social.user.domain.Password
import kotlin.test.assertTrue

class PasswordTest {
    @Test
    fun testPasswordCreation() {
        assertThrows<IllegalArgumentException> { Password.of("short") }
        assertThrows<IllegalArgumentException> { Password.of("lowercase") }
        assertThrows<IllegalArgumentException> { Password.of("UPPERCASE") }
        assertThrows<IllegalArgumentException> { Password.of("NoDigits") }
        assertThrows<IllegalArgumentException> { Password.of("NoSpecialChars1") }
        assertDoesNotThrow { Password.of("ValidPassword123!") }
    }

    @Test
    fun testPasswordMatching() {
        val it = Password.of("ValidPassword123!")
        val differentSalt = Password.of("ValidPassword123!")
        val sameSalt = Password.of(it.hash, false)
        assertTrue { it.match("ValidPassword123!") }
        assertTrue { it.match(sameSalt) }
        assertFalse { it.match(differentSalt) }
    }
}
