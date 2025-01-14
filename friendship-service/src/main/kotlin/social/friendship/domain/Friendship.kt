package social.friendship.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import social.common.ddd.AggregateRoot
import social.common.ddd.Factory
import social.common.ddd.ID
import social.friendship.domain.Friendship.FriendshipID
import social.friendship.domain.User.UserID

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
        fun of(user1: User, user2: User): Friendship {
            return createFriendship(user1, user2)
        }

        fun of(request: FriendshipRequest): Friendship {
            return createFriendship(request.to, request.from)
        }

        private fun createFriendship(user1: User, user2: User): Friendship {
            checkArguments(user1, user2)
            return if (user1.id.value < user2.id.value) {
                Friendship(user1, user2)
            } else {
                Friendship(user2, user1)
            }
        }

        private fun checkArguments(
            user1: User,
            user2: User
        ) {
            require(user1 != user2) { "User cannot be friend with itself" }
        }
    }
}
