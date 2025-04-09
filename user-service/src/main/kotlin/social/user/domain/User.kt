package social.user.domain

import social.common.ddd.Entity
import social.common.ddd.Factory

/**
 * Class to represent a user.
 * @param email the email of the user
 * @param username the username of the user
 */
class User private constructor(
    val email: String,
    val username: String,
    val isAdmin: Boolean,
    val isBlocked: Boolean,
) : Entity<UserID>(UserID.of(email)) {

    fun block() = of(this.email, this.username, this.isAdmin, true)
    fun unblock() = of(this.email, this.username, this.isAdmin, false)
    fun rename(username: String) = of(this.email, username, this.isAdmin, this.isBlocked)

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
        fun of(email: String, username: String, isAdmin: Boolean = false, isBlocked: Boolean = false): User =
            User(asId(email), username, isAdmin, isBlocked)

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
