package social.friendship.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import social.common.ddd.AggregateRoot
import social.common.ddd.Factory
import social.common.ddd.ID
import social.friendship.domain.Friendship.FriendshipID
import social.friendship.social.friendship.domain.User
import social.friendship.social.friendship.domain.User.UserID

class Friendship private constructor(
    @JsonProperty("to") val to: User,
    @JsonProperty("from") val from: User
) : AggregateRoot<FriendshipID>(FriendshipID(to.id, from.id)) {

    /**
     * Data class to represent the friendship ID.
     */
    data class FriendshipID @JsonCreator constructor(
        @JsonProperty("to") val to: UserID,
        @JsonProperty("from") val from: UserID
    ) : ID<Pair<UserID, UserID>>(Pair(to, from))

    companion object : Factory<Friendship> {
        fun of(to: User, from: User): Friendship {
            if (to == from) {
                throw IllegalArgumentException("User cannot be friend with itself")
            }
            return Friendship(to, from)
        }

        fun of(request: FriendshipRequest): Friendship = Friendship(request.to, request.from)
    }
}
