package social.user.domain

import social.common.ddd.AggregateRoot
import social.user.domain.User.UserID

class Credentials private constructor(user: UserID, val password: Password) : AggregateRoot<UserID>(user) {
    companion object {
        fun of(userID: UserID, password: Password) = Credentials(userID, password)
        fun of(userID: String, password: String, hash: Boolean = true) =
            Credentials(User.userIDOf(userID), Password.of(password, hash))
    }

    fun updatePassword(pwd: String) = of(id, Password.of(pwd))
}
