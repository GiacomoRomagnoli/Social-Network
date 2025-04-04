package social.friendship.domain

import social.common.ddd.AggregateRoot
import social.common.ddd.Factory
import social.common.ddd.ID
import social.friendship.domain.FriendshipRequest.FriendshipRequestID
import social.friendship.domain.User.UserID

/**
 * Class to represent a friendship request.
 */
class FriendshipRequest private constructor(
    val to: User,
    val from: User
) : AggregateRoot<FriendshipRequestID>(FriendshipRequestID(to.id, from.id)) {

    /**
     * Data class to represent the friendship request ID.
     */
    data class FriendshipRequestID(
        val to: UserID,
        val from: UserID
    ) : ID<Pair<String, String>>(Pair(to.value, from.value))

    /**
     * Factory object to create a friendship request.
     */
    companion object : Factory<FriendshipRequest> {
        /**
         * Creates a friendship request.
         *
         * @param to the user to whom the friendship request is sent
         * @param from the user who sends the friendship request
         * @return the friendship request
         * @throws IllegalArgumentException if the user sends a friendship request to itself
         */
        fun of(to: User, from: User): FriendshipRequest {
            if (to == from) {
                throw IllegalArgumentException("User cannot send a friendship request to itself")
            }
            return FriendshipRequest(to, from)
        }
    }
}
