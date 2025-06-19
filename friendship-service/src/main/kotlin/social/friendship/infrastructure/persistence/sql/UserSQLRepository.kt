package social.friendship.infrastructure.persistence.sql

import social.friendship.application.UserRepository
import social.friendship.domain.User
import social.friendship.domain.User.UserID
import social.friendship.infrastructure.persistence.sql.SQLUtils.prepareStatement

/**
 * SQL repository for users.
 */
class UserSQLRepository : UserRepository, AbstractSQLRepository() {
    /**
     * Find a user by ID.
     * @param id the ID of the user
     * @return the user if found, null otherwise
     */
    override fun findById(id: UserID): User? {
        prepareStatement(
            connection,
            SQLOperation.Query.SELECT_USER_BY_ID,
            id.value
        ).use { ps ->
            ps.executeQuery().use {
                return if (it.next())
                    User.of(it.getString(SQLColumns.UserTable.ID))
                else null
            }
        }
    }

    /**
     * Save a user.
     * @param entity the user to save
     */
    override fun save(entity: User) {
        prepareStatement(
            connection,
            SQLOperation.Update.INSERT_USER,
            entity.id.value
        ).use { it.executeUpdate() }
    }

    /**
     * Delete a user by ID.
     * @param id the ID of the user
     * @return the user if deleted, null otherwise
     */
    override fun deleteById(id: UserID): User? {
        prepareStatement(
            connection,
            SQLOperation.Update.DELETE_USER_BY_ID,
            id.value
        ).use { ps ->
            return if (ps.executeUpdate() > 0)
                User.of(id)
            else null
        }
    }

    /**
     * Find all users.
     * @return the list of all users
     */
    override fun findAll(): Array<User> {
        prepareStatement(
            connection,
            SQLOperation.Query.SELECT_ALL_USERS
        ).use { ps ->
            ps.executeQuery().use {
                val users = mutableListOf<User>()
                while (it.next()) {
                    users.add(User.of(it.getString(SQLColumns.UserTable.ID)))
                }
                return users.toTypedArray()
            }
        }
    }

    /**
     * Update a user.
     * @param entity the user to update
     */
    override fun update(entity: User) {
        prepareStatement(
            connection,
            SQLOperation.Update.UPDATE_USER,
            entity.id.value,
            entity.id.value
        ).use { it.executeUpdate() }
    }
}
