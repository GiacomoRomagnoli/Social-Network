package social.friendship.domain

import social.common.ddd.Entity
import social.common.ddd.Factory
import social.common.ddd.ID

/**
 * Class to represent a user.
 * @param id the user ID
 */
class User private constructor(id: UserID) : Entity<User.UserID>(id) {

    /**
     * Data class to represent the user ID.
     * @param value the value of the user ID
     */
    class UserID(value: String) : ID<String>(value)

    /**
     * Factory companion object to create a user.
     */
    companion object : Factory<User> {
        /**
         * Creates a user with the given user ID.
         * @param userID the user ID
         * @return the user
         */
        fun of(userID: String): User = User(UserID(userID))

        /**
         * Creates a user with the given user ID.
         * @param userID the user ID
         * @return the user
         */
        fun of(userID: UserID): User = User(userID)
    }
}
