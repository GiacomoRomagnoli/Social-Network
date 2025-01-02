package social.friendship.social.friendship.infrastructure.persistence.sql

import social.common.ddd.Repository
import social.friendship.domain.FriendshipRequest
import social.friendship.domain.FriendshipRequest.FriendshipRequestID
import social.friendship.social.friendship.domain.User
import java.sql.PreparedStatement

class FriendshipRequestSQLRepository : Repository<FriendshipRequestID, FriendshipRequest>, AbstractSQLRepository() {
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

    override fun update(entity: FriendshipRequest) {
        throw UnsupportedOperationException("Updates on friendship requests are not supported")
    }
}