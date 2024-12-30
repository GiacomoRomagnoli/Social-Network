package social.friendship.social.friendship.domain

import social.common.ddd.Entity
import social.common.ddd.Factory
import social.common.ddd.ID
import social.friendship.social.friendship.domain.User.UserID

class User private constructor(userID: UserID) : Entity<UserID>(userID) {
    data class UserID(val value: String) : ID<String>(value)

    companion object : Factory<User> {
        fun of(userID: String): User = User(UserID(userID))
        fun of(userID: UserID): User = User(userID)
    }
}
