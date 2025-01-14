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
    @JsonProperty("user1") val user1: User,
    @JsonProperty("user2") val user2: User
) : AggregateRoot<FriendshipID>(FriendshipID(user1.id, user2.id)) {

    /**
     * Data class to represent the friendship ID.
     */
    data class FriendshipID @JsonCreator constructor(
        @JsonProperty("user1") val user1: UserID,
        @JsonProperty("user2") val user2: UserID
    ) : ID<Pair<UserID, UserID>>(Pair(user1, user2))

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
