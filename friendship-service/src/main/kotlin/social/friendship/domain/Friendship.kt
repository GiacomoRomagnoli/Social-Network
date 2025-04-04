package social.friendship.domain

import social.common.ddd.AggregateRoot
import social.common.ddd.Factory
import social.common.ddd.ID
import social.friendship.domain.Friendship.FriendshipID
import social.friendship.domain.User.UserID

/**
 * Class to represent a friendship between two users.
 */
class Friendship(val user1: User, val user2: User) : AggregateRoot<FriendshipID>(FriendshipID(user1.id, user2.id)) {

    /**
     * Data class to represent the friendship ID.
     */
    data class FriendshipID(val user1: UserID, val user2: UserID) : ID<Pair<String, String>>(Pair(user1.value, user2.value))

    /**
     * Factory companion object to create a friendship.
     */
    companion object : Factory<Friendship> {
        /**
         * Creates a friendship between two users.
         *
         * @param user1 the first user.
         * @param user2 the second user.
         * @return the friendship.
         */
        fun of(user1: User, user2: User): Friendship {
            return createFriendship(user1, user2)
        }

        /**
         * Creates a friendship from a friendship request.
         *
         * @param request the friendship request.
         * @return the friendship.
         */
        fun of(request: FriendshipRequest): Friendship {
            return createFriendship(request.to, request.from)
        }

        /**
         * Creates a friendship between two users based on their IDs, checking if the friendship is valid and then
         * creating it generating the ID based on the users' IDs, in ascending order.
         */
        private fun createFriendship(user1: User, user2: User): Friendship {
            checkValidFriendship(user1, user2)
            return if (user1.id.value < user2.id.value) {
                Friendship(user1, user2)
            } else {
                Friendship(user2, user1)
            }
        }

        /**
         * Checks if the friendship is valid.
         */
        private fun checkValidFriendship(
            user1: User,
            user2: User
        ) {
            require(user1 != user2) { "User cannot be friend with itself" }
        }
    }
}
