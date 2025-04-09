package social.user.domain

import social.common.ddd.ID

class UserID private constructor(value: String) : ID<String>(value) {
    companion object {
        fun of(value: String) = UserID(value)
    }
}
