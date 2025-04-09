package social.user.infrastructure.persitence.sql

import social.user.application.CredentialsRepository
import social.user.domain.Credentials
import social.user.domain.UserID
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
            Credentials.fromHashed(result.getString(SQLColumns.USER_ID), result.getString(SQLColumns.PASSWORD))
        } else {
            null
        }
    }

    override fun save(entity: Credentials) {
        val credentials = entity.hashPassword()
        prepareStatement(
            connection,
            SQLOperation.INSERT_CREDENTIALS,
            credentials.id.value,
            credentials.password.value
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
                Credentials.fromHashed(
                    credentials.getString(SQLColumns.USER_ID),
                    credentials.getString(SQLColumns.PASSWORD)
                )
            )
        }
        return result.toTypedArray()
    }

    override fun update(entity: Credentials) {
        val credentials = entity.hashPassword()
        prepareStatement(
            connection,
            SQLOperation.UPDATE_CREDENTIALS,
            credentials.password.value,
            credentials.id.value
        ).executeUpdate()
    }
}
