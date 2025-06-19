package social.friendship.infrastructure.persistence.sql

import social.friendship.application.FriendshipRepository
import social.friendship.domain.Friendship
import social.friendship.domain.Friendship.FriendshipID
import social.friendship.domain.User
import social.friendship.infrastructure.persistence.sql.SQLUtils.prepareStatement

/**
 * SQL implementation of the FriendshipRepository.
 */
class FriendshipSQLRepository : FriendshipRepository, AbstractSQLRepository() {
    /**
     * Find a friendship by its ID.
     * @param id the ID of the friendship
     * @return the friendship if found, null otherwise
     */
    override fun findById(id: FriendshipID): Friendship? {
        prepareStatement(
            connection,
            SQLOperation.Query.SELECT_FRIENDSHIP_BY_ID,
            id.user1.value,
            id.user2.value
        ).use { ps ->
            ps.executeQuery().use {
                return if (it.next())
                    Friendship.of(
                        User.of(it.getString(SQLColumns.FriendshipTable.USER_1)),
                        User.of(it.getString(SQLColumns.FriendshipTable.USER_2))
                    )
                else null
            }
        }
    }

    /**
     * Save a friendship.
     * @param entity the friendship to save
     */
    override fun save(entity: Friendship) {
        prepareStatement(
            connection,
            SQLOperation.Update.INSERT_FRIENDSHIP,
            entity.user1.id.value,
            entity.user2.id.value
        ).use { it.executeUpdate() }
    }

    /**
     * Delete a friendship by its ID.
     * @param id the ID of the friendship
     * @return the deleted friendship if found, null otherwise
     */
    override fun deleteById(id: FriendshipID): Friendship? {
        prepareStatement(
            connection,
            SQLOperation.Update.DELETE_FRIENDSHIP_BY_ID,
            id.user1.value,
            id.user2.value
        ).use { ps ->
            return if (ps.executeUpdate() > 0)
                Friendship.of(User.of(id.user1), User.of(id.user2))
            else null
        }
    }

    /**
     * Find all friendships.
     * @return all friendships
     */
    override fun findAll(): Array<Friendship> {
        prepareStatement(
            connection,
            SQLOperation.Query.SELECT_ALL_FRIENDSHIPS
        ).use { ps ->
            ps.executeQuery().use {
                val friendships = mutableListOf<Friendship>()
                while (it.next()) {
                    friendships.add(
                        Friendship.of(
                            User.of(it.getString(SQLColumns.FriendshipTable.USER_1)),
                            User.of(it.getString(SQLColumns.FriendshipTable.USER_2))
                        )
                    )
                }
                return friendships.toTypedArray()
            }
        }
    }

    /**
     * Find all friendships of a user.
     * @param userID the ID of the user
     * @return all friendships of the user
     */
    override fun findAllFriendsOf(userID: User.UserID): Iterable<User> {
        prepareStatement(
            connection,
            SQLOperation.Query.SELECT_FRIENDSHIPS_OF_USER,
            userID.value,
            userID.value
        ).use { ps ->
            ps.executeQuery().use {
                val friends = mutableListOf<User>()
                while (it.next()) {
                    val users = listOf(
                        User.of(it.getString(SQLColumns.FriendshipTable.USER_1)),
                        User.of(it.getString(SQLColumns.FriendshipTable.USER_2))
                    )
                    users.filter { u -> u.id != userID }
                        .forEach { f -> friends.add(f) }
                }
                return friends.toList()
            }
        }
    }

    /**
     * Update a friendship.
     * @param entity the friendship to update
     */
    override fun update(entity: Friendship) {
        prepareStatement(
            connection,
            SQLOperation.Update.UPDATE_FRIENDSHIP,
            entity.user1.id.value,
            entity.user2.id.value,
            entity.user1.id.value,
            entity.user2.id.value
        ).use { it.executeUpdate() }
    }
}
