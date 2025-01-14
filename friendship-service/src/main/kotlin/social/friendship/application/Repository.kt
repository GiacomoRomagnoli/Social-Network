package social.friendship.application

import social.common.ddd.Repository
import social.friendship.domain.Friendship
import social.friendship.domain.Friendship.FriendshipID
import social.friendship.domain.FriendshipRequest
import social.friendship.domain.FriendshipRequest.FriendshipRequestID
import social.friendship.domain.Message
import social.friendship.domain.Message.MessageID
import social.friendship.domain.User
import social.friendship.domain.User.UserID

interface UserRepository : Repository<UserID, User>

interface FriendshipRequestRepository : Repository<FriendshipRequestID, FriendshipRequest> {
    fun getAllFriendshipRequestsOf(userId: UserID): Iterable<FriendshipRequest>
}

interface FriendshipRepository : Repository<FriendshipID, Friendship> {
    fun findAllFriendsOf(userID: UserID): Iterable<User>
}

interface MessageRepository : Repository<MessageID, Message> {
    fun findAllMessagesReceivedBy(userID: UserID): Iterable<Message>
    fun findAllMessagesExchangedBetween(user1: UserID, user2: UserID): Iterable<Message>
}
