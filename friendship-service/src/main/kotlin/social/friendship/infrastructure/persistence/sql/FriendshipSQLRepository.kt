package social.friendship.social.friendship.infrastructure.persistence.sql

import social.common.ddd.Repository
import social.friendship.domain.Friendship
import social.friendship.domain.Friendship.FriendshipID
import social.friendship.social.friendship.domain.User
import java.sql.PreparedStatement

class FriendshipSQLRepository : Repository<FriendshipID, Friendship>, AbstractSQLRepository() {
    override fun findById(id: FriendshipID): Friendship? {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Query.SELECT_FRIENDSHIP_BY_ID,
            id.to.value,
            id.from.value
        )
        val result = ps.executeQuery()
        return if (result.next()) {
            Friendship.of(User.of(result.getString(SQLColumns.FriendshipTable.TO)), User.of(result.getString(SQLColumns.FriendshipTable.FROM)))
        } else {
            null
        }
    }

    override fun save(entity: Friendship) {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Update.INSERT_FRIENDSHIP,
            entity.to.id.value,
            entity.from.id.value
        )
        ps.executeUpdate()
    }

    override fun deleteById(id: FriendshipID): Friendship? {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Update.DELETE_FRIENDSHIP_BY_ID,
            id.to.value,
            id.from.value
        )
        val result = ps.executeUpdate()
        return if (result > 0) {
            Friendship.of(User.of(id.to), User.of(id.from))
        } else {
            null
        }
    }

    override fun findAll(): Array<Friendship> {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Query.SELECT_ALL_FRIENDSHIPS
        )
        val result = ps.executeQuery()
        val friendships = mutableListOf<Friendship>()
        while (result.next()) {
            friendships.add(Friendship.of(User.of((result.getString(SQLColumns.FriendshipTable.TO))), User.of((result.getString(SQLColumns.FriendshipTable.FROM)))))
        }
        return friendships.toTypedArray()
    }

    override fun update(entity: Friendship) {
        throw UnsupportedOperationException("Updates on friendships are not supported")
    }
}
