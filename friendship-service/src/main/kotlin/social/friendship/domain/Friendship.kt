package social.friendship.social.friendship.domain

import social.common.ddd.Entity
import social.common.ddd.ID

class Friendship private constructor(val to: String, val from: String) : Entity<Friendship.FriendshipID>(FriendshipID(to, from)) {
    /**
     * Data class to represent the friendship ID.
     */
    data class FriendshipID(val to: String, val from: String) : ID<Pair<String, String>>(Pair(to, from))

    companion object {
        fun of(to: String, from: String): Friendship = Friendship(to, from)
    }
}
