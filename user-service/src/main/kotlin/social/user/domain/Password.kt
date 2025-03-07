package social.user.domain

import org.mindrot.jbcrypt.BCrypt
import social.common.ddd.ValueObject

/**
 * Class to represent a Password
 * @param hash the hashed password string
 */
class Password private constructor(val hash: String) : ValueObject {
    companion object {
        private const val SPECIAL_CHARS = "!@#\$%^&*()_+\\-=\\[\\]{}|;:'\",.<>?/"

        /**
         * Factory method for Password creation
         * @param password the string representing the password
         * @param hash flag to determine if password has to be hashed
         * @return a Password object
         */
        fun of(password: String, hash: Boolean = true): Password {
            validate(password)
            return if (hash) {
                Password(BCrypt.hashpw(password, BCrypt.gensalt()))
            } else {
                Password(password)
            }
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
    fun match(password: String) = BCrypt.checkpw(password, hash)

    /**
     * Check if a Password object matches this Password
     * @param hashedPassword a Password to be matched
     * @return true if passwords match, false otherwise
     */
    fun match(hashedPassword: Password) = hashedPassword.hash == hash
}
