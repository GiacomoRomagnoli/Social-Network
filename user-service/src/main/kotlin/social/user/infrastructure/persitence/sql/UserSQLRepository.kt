package social.user.infrastructure.persitence.sql

import com.mysql.cj.jdbc.exceptions.CommunicationsException
import org.apache.logging.log4j.LogManager
import social.user.application.UserRepository
import social.user.domain.User
import social.user.domain.UserID
import social.user.infrastructure.persitence.sql.SQLUtils.prepareStatement
import social.user.infrastructure.probes.SyncProbe
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLIntegrityConstraintViolationException

/**
 * SQL repository for users.
 */
class UserSQLRepository : UserRepository, SyncProbe {
    private val logger = LogManager.getLogger(UserSQLRepository::class)
    private lateinit var connection: Connection

    /**
     * Connect to a database.
     * @param host the host of the database
     * @param port the port of the database
     * @param database the name of the database
     * @param username the username to connect to the database
     * @param password the password to connect to the database
     */
    fun connect(host: String, port: String, database: String, username: String, password: String) {
        logger.trace(
            "Connecting to database with credentials:\n" +
                "host={},\n" +
                "port={},\n" +
                "database={},\n" +
                "username={}",
            host, port, database, username
        )
        connection = SQLUtils.mySQLConnection(host, port, database, username, password)
    }

    override fun userCount(): Int {
        prepareStatement(connection, SQLOperation.USER_COUNT).use { ps ->
            ps.executeQuery().use {
                it.next()
                return it.getInt(1)
            }
        }
    }

    /**
     * Find a user by ID.
     */
    override fun findById(id: UserID): User? {
        prepareStatement(
            connection,
            SQLOperation.SELECT_USER_BY_ID,
            id.value
        ).use { ps ->
            ps.executeQuery().use {
                return if (it.next()) {
                    User.of(
                        it.getString(SQLColumns.EMAIL),
                        it.getString(SQLColumns.USERNAME),
                        it.getBoolean(SQLColumns.ADMIN),
                        it.getBoolean(SQLColumns.BLOCKED)
                    )
                } else {
                    null
                }
            }
        }
    }

    /**
     * Save a user into the database.
     */
    override fun save(entity: User) {
        prepareStatement(
            connection,
            SQLOperation.INSERT_USER,
            entity.email,
            entity.username,
            entity.isAdmin,
            entity.isBlocked
        ).use { it.executeUpdate() }
    }

    /**
     * Delete a user by ID.
     */
    override fun deleteById(id: UserID): User? {
        val userToDelete = findById(id) ?: return null
        prepareStatement(
            connection,
            SQLOperation.DELETE_USER_BY_ID,
            id.value
        ).use {
            return if (it.executeUpdate() > 0) {
                userToDelete
            } else {
                null
            }
        }
    }

    /**
     * Find all users.
     */
    override fun findAll(): Array<User> {
        prepareStatement(
            connection,
            SQLOperation.SELECT_ALL_USERS
        ).use { ps ->
            ps.executeQuery().use {
                val users = mutableListOf<User>()
                while (it.next()) {
                    users.add(
                        User.of(
                            it.getString(SQLColumns.EMAIL),
                            it.getString(SQLColumns.USERNAME),
                            it.getBoolean(SQLColumns.ADMIN),
                            it.getBoolean(SQLColumns.BLOCKED)
                        )
                    )
                }
                return users.toTypedArray()
            }
        }
    }

    /**
     * Update a user.
     */
    override fun update(entity: User) {
        prepareStatement(
            connection,
            SQLOperation.UPDATE_USER,
            entity.username,
            entity.isAdmin,
            entity.isBlocked,
            entity.email
        ).use { ps ->
            if (ps.executeUpdate() == 0) {
                throw SQLIntegrityConstraintViolationException("no rows affected")
            }
        }
    }

    override fun isReady(): Unit =
        prepareStatement(connection, "SELECT 1").use { ps ->
            ps.executeQuery().use { it.next() }
        }
}

/**
 * SQL utilities.
 */
object SQLUtils {
    private val logger = LogManager.getLogger(SQLUtils::class)

    /**
     * Prepare a statement with parameters.
     */
    fun prepareStatement(connection: Connection, sqlStatement: String, vararg params: Any): PreparedStatement {
        val ps = connection.prepareStatement(sqlStatement)
        params.forEachIndexed { index, param ->
            ps.setObject(index + 1, param)
        }
        return ps
    }

    /**
     * Connect to a MySQL database.
     */
    fun mySQLConnection(
        host: String,
        port: String,
        database: String,
        username: String,
        password: String
    ): Connection {
        val url = "jdbc:mysql://$host:$port/$database"
        logger.trace("Attempting to connect to database with URL: {}", url)
        try {
            val conn: Connection = DriverManager.getConnection(url, username, password)
            logger.trace("Connection established successfully")
            return conn
        } catch (e: CommunicationsException) {
            logger.error("Failed to connect to database with URL: {}: {}", url, e)
            throw e
        }
    }
}
