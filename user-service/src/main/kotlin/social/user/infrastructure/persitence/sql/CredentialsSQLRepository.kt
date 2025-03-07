package social.user.infrastructure.persitence.sql

import social.user.application.CredentialsRepository
import social.user.domain.Credentials
import social.user.domain.User.UserID
import social.user.infrastructure.persitence.sql.SQLUtils.mySQLConnection
import social.user.infrastructure.persitence.sql.SQLUtils.prepareStatement
import java.sql.Connection

class CredentialsSQLRepository(private val connection: Connection) : CredentialsRepository {
    companion object {
        fun of(host: String, port: String, database: String, username: String, password: String) =
            CredentialsSQLRepository(mySQLConnection(host, port, database, username, password))
    }

    override fun findById(id: UserID): Credentials? {
        val result = prepareStatement(
            connection,
            SQLOperation.SELECT_CREDENTIALS_BY_ID,
            id.value
        ).executeQuery()
        return if (result.next()) {
            Credentials.of(
                result.getString(SQLColumns.USER_ID),
                result.getString(SQLColumns.PASSWORD),
                false
            )
        } else {
            null
        }
    }

    override fun save(entity: Credentials) {
        prepareStatement(
            connection,
            SQLOperation.INSERT_CREDENTIALS,
            entity.id.value,
            entity.password.hash
        ).executeUpdate()
    }

    override fun deleteById(id: UserID): Credentials? {
        val result = findById(id)
        if (result != null) {
            prepareStatement(
                connection,
                SQLOperation.DELETE_CREDENTIALS_BY_ID,
                id.value
            ).executeUpdate()
        }
        return result
    }

    override fun findAll(): Array<Credentials> {
        val credentials = prepareStatement(
            connection,
            SQLOperation.SELECT_ALL_CREDENTIALS
        ).executeQuery()
        val result = mutableListOf<Credentials>()
        while (credentials.next()) {
            result.add(
                Credentials.of(
                    credentials.getString(SQLColumns.USER_ID),
                    credentials.getString(SQLColumns.PASSWORD),
                    false
                )
            )
        }
        return result.toTypedArray()
    }

    override fun update(entity: Credentials) {
        prepareStatement(
            connection,
            SQLOperation.UPDATE_CREDENTIALS,
            entity.password.hash,
            entity.id.value
        ).executeUpdate()
    }
}
