package social.user.domain

import social.common.ddd.AggregateRoot

class Credentials private constructor(user: UserID, val password: Password) : AggregateRoot<UserID>(user) {
    companion object {
        fun of(id: UserID, password: Password) = Credentials(id, password)
        fun of(id: String, password: Password) = of(UserID.of(id), password)
        fun of(id: UserID, password: String) = of(id, Password.of(password))
        fun of(id: String, password: String) = of(UserID.of(id), Password.of(password))
        fun fromHashed(id: UserID, hashedPassword: String) = of(id, Password.hashed(hashedPassword))
        fun fromHashed(id: String, hashedPassword: String) = fromHashed(UserID.of(id), hashedPassword)
    }

    fun updatePassword(pwd: String) = of(id, Password.of(pwd))
    fun hashPassword() = of(id, password.hash())
}
