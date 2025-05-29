package social.user.infrastructure.persitence.sql

import social.user.application.CredentialsRepository
import social.user.domain.Credentials
import social.user.domain.UserID
import social.user.infrastructure.persitence.sql.SQLUtils.mySQLConnection
import social.user.infrastructure.persitence.sql.SQLUtils.prepareStatement
import social.user.infrastructure.probes.SyncProbe
import java.sql.Connection

class CredentialsSQLRepository(private val connection: Connection) : CredentialsRepository, SyncProbe {
    companion object {
        fun of(host: String, port: String, database: String, username: String, password: String) =
            CredentialsSQLRepository(mySQLConnection(host, port, database, username, password))
    }

    override fun findById(id: UserID): Credentials? {
        prepareStatement(
            connection,
            SQLOperation.SELECT_CREDENTIALS_BY_ID,
            id.value
        ).use { ps ->
            ps.executeQuery().use {
                return if (it.next()) {
                    Credentials.fromHashed(it.getString(SQLColumns.USER_ID), it.getString(SQLColumns.PASSWORD))
                } else {
                    null
                }
            }
        }
    }

    override fun save(entity: Credentials) {
        val credentials = entity.hashPassword()
        prepareStatement(
            connection,
            SQLOperation.INSERT_CREDENTIALS,
            credentials.id.value,
            credentials.password.value
        ).use { it.executeUpdate() }
    }

    override fun deleteById(id: UserID): Credentials? {
        val result = findById(id)
        if (result != null) {
            prepareStatement(
                connection,
                SQLOperation.DELETE_CREDENTIALS_BY_ID,
                id.value
            ).use { it.executeUpdate() }
        }
        return result
    }

    override fun findAll(): Array<Credentials> {
        prepareStatement(
            connection,
            SQLOperation.SELECT_ALL_CREDENTIALS
        ).use { ps ->
            ps.executeQuery().use {
                val result = mutableListOf<Credentials>()
                while (it.next()) {
                    result.add(
                        Credentials.fromHashed(
                            it.getString(SQLColumns.USER_ID),
                            it.getString(SQLColumns.PASSWORD)
                        )
                    )
                }
                return result.toTypedArray()
            }
        }
    }

    override fun update(entity: Credentials) {
        val credentials = entity.hashPassword()
        prepareStatement(
            connection,
            SQLOperation.UPDATE_CREDENTIALS,
            credentials.password.value,
            credentials.id.value
        ).use { it.executeUpdate() }
    }

    override fun isReady(): Unit =
        prepareStatement(connection, "SELECT 1").use { ps ->
            ps.executeQuery().use { it.next() }
        }
}
