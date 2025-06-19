package social.friendship.infrastructure.persistence.sql

import org.apache.logging.log4j.LogManager
import social.friendship.application.FriendshipRequestRepository
import social.friendship.domain.FriendshipRequest
import social.friendship.domain.FriendshipRequest.FriendshipRequestID
import social.friendship.domain.User
import social.friendship.infrastructure.persistence.sql.SQLUtils.prepareStatement

/**
 * SQL repository for friendship requests.
 */
class FriendshipRequestSQLRepository : FriendshipRequestRepository, AbstractSQLRepository() {
    val logger = LogManager.getLogger(this::class)

    /**
     * Find a friendship request by its ID.
     * @param id the ID of the friendship request
     * @return the friendship request if found, null otherwise
     */
    override fun findById(id: FriendshipRequestID): FriendshipRequest? {
        prepareStatement(
            connection,
            SQLOperation.Query.SELECT_FRIENDSHIP_REQUEST_BY_ID,
            id.to.value,
            id.from.value
        ).use { ps ->
            ps.executeQuery().use {
                return if (it.next())
                    FriendshipRequest.of(
                        User.of(it.getString(SQLColumns.FriendshipRequestTable.TO)),
                        User.of(it.getString(SQLColumns.FriendshipRequestTable.FROM))
                    )
                else null
            }
        }
    }

    /**
     * Save a friendship request.
     * @param entity the friendship request to save
     */
    override fun save(entity: FriendshipRequest) {
        prepareStatement(
            connection,
            SQLOperation.Update.INSERT_FRIENDSHIP_REQUEST,
            entity.to.id.value,
            entity.from.id.value
        ).use { it.executeUpdate() }
    }

    /**
     * Delete a friendship request by its ID.
     * @param id the ID of the friendship request
     * @return the deleted friendship request if found, null otherwise
     */
    override fun deleteById(id: FriendshipRequestID): FriendshipRequest? {
        prepareStatement(
            connection,
            SQLOperation.Update.DELETE_FRIENDSHIP_REQUEST_BY_ID,
            id.to.value,
            id.from.value
        ).use {
            return if (it.executeUpdate() > 0)
                FriendshipRequest.of(User.of(id.to), User.of(id.from))
            else null
        }
    }

    /**
     * Find all friendship requests.
     * @return all friendship requests
     */
    override fun findAll(): Array<FriendshipRequest> {
        prepareStatement(connection, SQLOperation.Query.SELECT_ALL_FRIENDSHIP_REQUESTS).use { ps ->
            ps.executeQuery().use {
                val fr = mutableListOf<FriendshipRequest>()
                while (it.next()) {
                    fr.add(
                        FriendshipRequest.of(
                            User.of(it.getString(SQLColumns.FriendshipRequestTable.TO)),
                            User.of(it.getString(SQLColumns.FriendshipRequestTable.FROM))
                        )
                    )
                }
                return fr.toTypedArray()
            }
        }
    }

    /**
     * Get all friendship requests of a user.
     * @param userId the ID of the user
     * @return all friendship requests of the user
     */
    override fun getAllFriendshipRequestsOf(userId: User.UserID): Iterable<FriendshipRequest> {
        prepareStatement(
            connection,
            SQLOperation.Query.SELECT_FRIENDSHIP_REQUESTS_OF_USER,
            userId.value,
            userId.value,
        ).use { ps ->
            ps.executeQuery().use {
                val fr = mutableListOf<FriendshipRequest>()
                while (it.next()) {
                    fr.add(
                        FriendshipRequest.of(
                            User.of(it.getString(SQLColumns.FriendshipRequestTable.TO)),
                            User.of(it.getString(SQLColumns.FriendshipRequestTable.FROM))
                        )
                    )
                }
                return fr.toList()
            }
        }
    }

    /**
     * Update a friendship request.
     * @param entity the friendship request to update
     */
    override fun update(entity: FriendshipRequest) {
        prepareStatement(
            connection,
            SQLOperation.Update.UPDATE_FRIENDSHIP_REQUEST,
            entity.to.id.value,
            entity.from.id.value,
            entity.to.id.value,
            entity.from.id.value
        ).use { it.executeUpdate() }
    }
}
