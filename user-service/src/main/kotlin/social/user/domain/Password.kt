package social.user.domain

import org.mindrot.jbcrypt.BCrypt
import social.common.ddd.ValueObject

/**
 * Class to represent a Password
 * @param value the hashed password string
 */
class Password private constructor(val value: String, private val isHashed: Boolean) : ValueObject {
    companion object {
        private const val SPECIAL_CHARS = "!@#\$%^&*()_+\\-=\\[\\]{}|;:'\",.<>?/"
        private val BCRYPT_PREFIXES = listOf("\$2a\$", "\$2b\$", "\$2y\$")

        /**
         * Factory method for Password creation
         * @param password the string representing the password
         * @return a Password object
         */
        fun of(password: String): Password {
            validate(password)
            return Password(password, false)
        }

        fun hashed(password: String): Password {
            require(BCRYPT_PREFIXES.any { password.startsWith(it) })
            return Password(password, true)
        }

        /**
         * Check if a string is a valid password
         * @param password the string to be validated
         * @throws IllegalArgumentException when password does not respect safety criteria
         */
        private fun validate(password: String) = when {
            password.length < 8 ->
                throw IllegalArgumentException("password must have at least 8 characters")
            !password.any { it.isUpperCase() } ->
                throw IllegalArgumentException("password must contain uppercase characters")
            !password.any { it.isLowerCase() } ->
                throw IllegalArgumentException("password must contain lowercase characters")
            !password.any { it.isDigit() } ->
                throw IllegalArgumentException("password must contain digits")
            !password.any { it in SPECIAL_CHARS } ->
                throw IllegalArgumentException("password must contain special characters")
            else -> Unit
        }
    }

    /**
     * Check if a string matches the Password
     * @param password the string to be matched
     * @return true if the string match, false otherwise
     */
    fun match(password: String) =
        if (this.isHashed) BCrypt.checkpw(password, this.value) else password == this.value

    /**
     * Check if a Password object matches this Password
     * the match is possible only if at least one between password and this
     * is not hashed
     * @param password a Password to be matched
     * @return true if passwords match, false otherwise
     */
    fun match(password: Password) = when {
        !password.isHashed -> match(password.value)
        !this.isHashed -> password.match(this.value)
        else -> this.value == password.value
    }

    /**
     * @return if this is hashed this, else a new Password with same value but hashed
     */
    fun hash() = if (isHashed) this else Password(BCrypt.hashpw(value, BCrypt.gensalt()), true)
}
