package social.user.domain

import social.common.ddd.Entity
import social.common.ddd.Factory
import social.common.ddd.ID

/**
 * Class to represent a user.
 * @param email the email of the user
 * @param username the username of the user
 */
// TODO: aggiungere flag blocked e flag admin
class User private constructor(val email: String, val username: String) : Entity<User.UserID>(UserID(email)) {
    /**
     * Data class to represent the user ID.
     */
    data class UserID(val value: String) : ID<String>(value)

    /**
     * Factory companion object to create a user.
     */
    companion object : Factory<User> {
        /**
         * Creates a user with the given email and username.
         * @param email the email of the user
         * @param username the username of the user
         * @return the user
         */
        fun of(email: String, username: String): User = User(asId(email), username)

        /**
         * Creates used ID from email.
         * @param email the email of the user
         * @return the user
         */
        fun userIDOf(email: String): UserID = UserID(asId(email))

        /**
         * Converts email to user ID.
         * @param email the email of the user
         * @return the user ID
         */
        private fun asId(email: String): String {
            // check if the email is valid, if no match is found, returns null, else returns the email as UserID
            return Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}\$")
                .find(email)?.value ?: throw IllegalArgumentException("Invalid email")
        }
    }
}
