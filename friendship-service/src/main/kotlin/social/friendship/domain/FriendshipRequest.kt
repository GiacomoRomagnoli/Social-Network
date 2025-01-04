package social.friendship.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import social.common.ddd.AggregateRoot
import social.common.ddd.Factory
import social.common.ddd.ID
import social.friendship.domain.FriendshipRequest.FriendshipRequestID
import social.friendship.social.friendship.domain.User
import social.friendship.social.friendship.domain.User.UserID

class FriendshipRequest private constructor(
    @JsonProperty("to") val to: User,
    @JsonProperty("from") val from: User
) : AggregateRoot<FriendshipRequestID>(FriendshipRequestID(to.id, from.id)) {

    /**
     * Data class to represent the friendship request ID.
     */
    data class FriendshipRequestID @JsonCreator constructor(
        @JsonProperty("to") val to: UserID,
        @JsonProperty("from") val from: UserID
    ) : ID<Pair<UserID, UserID>>(Pair(to, from))

    companion object : Factory<FriendshipRequest> {
        fun of(to: User, from: User): FriendshipRequest = FriendshipRequest(to, from)
    }
}
