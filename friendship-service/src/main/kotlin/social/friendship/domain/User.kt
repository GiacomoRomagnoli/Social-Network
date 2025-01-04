package social.friendship.social.friendship.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import social.common.ddd.Entity
import social.common.ddd.Factory
import social.common.ddd.ID
import social.friendship.social.friendship.domain.User.UserID

class User private constructor(
    @JsonProperty("userId") val userId: UserID
) : Entity<UserID>(userId) {

    data class UserID @JsonCreator constructor(
        @JsonProperty("value") val value: String
    ) : ID<String>(value)

    companion object : Factory<User> {
        fun of(userID: String): User = User(UserID(userID))
        fun of(userID: UserID): User = User(userID)
    }
}
