package social.friendship.social.friendship.infrastructure.persistence.sql

import social.common.ddd.Repository
import social.friendship.social.friendship.domain.User
import social.friendship.social.friendship.domain.User.UserID
import java.sql.PreparedStatement

class UserSQLRepository: Repository<UserID, User>, AbstractSQLRepository() {
    override fun findById(id: UserID): User? {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Query.SELECT_USER_BY_ID,
            id.value
        )
        val result = ps.executeQuery()
        return if (result.next()) {
            User.of(result.getString(SQLColumns.UserTable.ID))
        } else {
            null
        }
    }

    override fun save(entity: User) {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Update.INSERT_USER,
            entity.id.value
        )
        ps.executeUpdate()
    }

    override fun deleteById(id: UserID): User? {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Update.DELETE_USER_BY_ID,
            id.value
        )
        val result = ps.executeUpdate()
        return if (result > 0) {
            User.of(id)
        } else {
            null
        }
    }

    override fun findAll(): Array<User> {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Query.SELECT_ALL_USERS
        )
        val result = ps.executeQuery()
        val users = mutableListOf<User>()
        while (result.next()) {
            users.add(User.of(result.getString(SQLColumns.UserTable.ID)))
        }
        return users.toTypedArray()
    }

    override fun update(entity: User) {
        throw UnsupportedOperationException("Updates on users are not supported")
    }
}
