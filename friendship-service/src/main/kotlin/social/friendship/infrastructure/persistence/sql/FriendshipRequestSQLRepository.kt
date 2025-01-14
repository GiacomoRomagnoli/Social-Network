package social.friendship.infrastructure.persistence.sql

import org.apache.logging.log4j.LogManager
import social.friendship.application.FriendshipRequestRepository
import social.friendship.domain.FriendshipRequest
import social.friendship.domain.FriendshipRequest.FriendshipRequestID
import social.friendship.domain.User
import java.sql.PreparedStatement

class FriendshipRequestSQLRepository : FriendshipRequestRepository, AbstractSQLRepository() {
    val logger = LogManager.getLogger(this::class)

    override fun findById(id: FriendshipRequestID): FriendshipRequest? {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Query.SELECT_FRIENDSHIP_REQUEST_BY_ID,
            id.to.value,
            id.from.value
        )
        val result = ps.executeQuery()
        return if (result.next()) {
            FriendshipRequest.of(User.of(result.getString(SQLColumns.FriendshipRequestTable.TO)), User.of(result.getString(SQLColumns.FriendshipRequestTable.FROM)))
        } else {
            null
        }
    }

    override fun save(entity: FriendshipRequest) {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Update.INSERT_FRIENDSHIP_REQUEST,
            entity.to.id.value,
            entity.from.id.value
        )
        ps.executeUpdate()
    }

    override fun deleteById(id: FriendshipRequestID): FriendshipRequest? {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Update.DELETE_FRIENDSHIP_REQUEST_BY_ID,
            id.to.value,
            id.from.value
        )
        val result = ps.executeUpdate()
        return if (result > 0) {
            FriendshipRequest.of(User.of(id.to), User.of(id.from))
        } else {
            null
        }
    }

    override fun findAll(): Array<FriendshipRequest> {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Query.SELECT_ALL_FRIENDSHIP_REQUESTS
        )
        val result = ps.executeQuery()
        val friendshipRequests = mutableListOf<FriendshipRequest>()
        while (result.next()) {
            friendshipRequests.add(FriendshipRequest.of(User.of(result.getString(SQLColumns.FriendshipRequestTable.TO)), User.of(result.getString(SQLColumns.FriendshipRequestTable.FROM))))
        }
        return friendshipRequests.toTypedArray()
    }

    override fun getAllFriendshipRequestsOf(userId: User.UserID): Iterable<FriendshipRequest> {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Query.SELECT_FRIENDSHIP_REQUESTS_OF_USER,
            userId.value,
            userId.value,
        )
        val result = ps.executeQuery()
        val friendshipRequests = mutableListOf<FriendshipRequest>()
        while (result.next()) {
            val userTo = User.of(result.getString(SQLColumns.FriendshipRequestTable.TO))
            val userFrom = User.of(result.getString(SQLColumns.FriendshipRequestTable.FROM))
            val friendshipRequest = FriendshipRequest.of(userTo, userFrom)
            friendshipRequests.add(friendshipRequest)
        }
        return friendshipRequests.toList()
    }

    override fun update(entity: FriendshipRequest) {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Update.UPDATE_FRIENDSHIP_REQUEST,
            entity.to.id.value,
            entity.from.id.value,
            entity.to.id.value,
            entity.from.id.value
        )
        ps.executeUpdate()
    }
}
