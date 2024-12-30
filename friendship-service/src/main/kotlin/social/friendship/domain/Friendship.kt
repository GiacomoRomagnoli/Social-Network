package social.friendship.domain

import social.common.ddd.AggregateRoot
import social.common.ddd.Factory
import social.common.ddd.ID
import social.friendship.domain.Friendship.FriendshipID
import social.friendship.social.friendship.domain.User
import social.friendship.social.friendship.domain.User.UserID

class Friendship private constructor(val to: User, val from: User) : AggregateRoot<FriendshipID>(FriendshipID(to.id, from.id)) {
    /**
     * Data class to represent the friendship ID.
     */
    data class FriendshipID(val to: UserID, val from: UserID) : ID<Pair<UserID, UserID>>(Pair(to, from))

    companion object : Factory<Friendship> {
        fun of(to: User, from: User): Friendship = Friendship(to, from)
        fun of(request: FriendshipRequest): Friendship = Friendship(request.to, request.from)
    }
}
