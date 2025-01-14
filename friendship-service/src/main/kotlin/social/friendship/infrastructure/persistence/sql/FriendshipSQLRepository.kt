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
            id.user1.value,
            id.user2.value
        )
        val result = ps.executeQuery()
        return if (result.next()) {
            Friendship.of(User.of(result.getString(SQLColumns.FriendshipTable.USER_1)), User.of(result.getString(SQLColumns.FriendshipTable.USER_2)))
        } else {
            null
        }
    }

    override fun save(entity: Friendship) {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Update.INSERT_FRIENDSHIP,
            entity.user1.id.value,
            entity.user2.id.value
        )
        ps.executeUpdate()
    }

    override fun deleteById(id: FriendshipID): Friendship? {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Update.DELETE_FRIENDSHIP_BY_ID,
            id.user1.value,
            id.user2.value
        )
        val result = ps.executeUpdate()
        return if (result > 0) {
            Friendship.of(User.of(id.user1), User.of(id.user2))
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
            friendships.add(Friendship.of(User.of((result.getString(SQLColumns.FriendshipTable.USER_1))), User.of((result.getString(SQLColumns.FriendshipTable.USER_2)))))
        }
        return friendships.toTypedArray()
    }

    override fun update(entity: Friendship) {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Update.UPDATE_FRIENDSHIP,
            entity.user1.id.value,
            entity.user2.id.value,
            entity.user1.id.value,
            entity.user2.id.value
        )
        ps.executeUpdate()
    }
}
