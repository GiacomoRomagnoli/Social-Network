package social.user.infrastructure.persitence.sql

import com.mysql.cj.jdbc.exceptions.CommunicationsException
import org.apache.logging.log4j.LogManager
import social.user.application.UserRepository
import social.user.domain.User
import social.user.domain.UserID
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLIntegrityConstraintViolationException

/**
 * SQL repository for users.
 */
class UserSQLRepository : UserRepository {
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

    /**
     * Find a user by ID.
     */
    override fun findById(id: UserID): User? {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.SELECT_USER_BY_ID,
            id.value
        )
        val rs = ps.executeQuery()
        return if (rs.next()) {
            User.of(
                rs.getString(SQLColumns.EMAIL),
                rs.getString(SQLColumns.USERNAME),
                rs.getBoolean(SQLColumns.ADMIN),
                rs.getBoolean(SQLColumns.BLOCKED)
            )
        } else {
            null
        }
    }

    /**
     * Save a user into the database.
     */
    override fun save(entity: User) {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.INSERT_USER,
            entity.email,
            entity.username,
            entity.isAdmin,
            entity.isBlocked
        )
        ps.executeUpdate()
    }

    /**
     * Delete a user by ID.
     */
    override fun deleteById(id: UserID): User? {
        val userToDelete = findById(id) ?: return null
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.DELETE_USER_BY_ID,
            id.value
        )
        val result = ps.executeUpdate()
        return if (result > 0) {
            userToDelete
        } else {
            null
        }
    }

    /**
     * Find all users.
     */
    override fun findAll(): Array<User> {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.SELECT_ALL_USERS
        )
        val rs = ps.executeQuery()
        val users = mutableListOf<User>()
        while (rs.next()) {
            users.add(
                User.of(
                    rs.getString(SQLColumns.EMAIL),
                    rs.getString(SQLColumns.USERNAME),
                    rs.getBoolean(SQLColumns.ADMIN),
                    rs.getBoolean(SQLColumns.BLOCKED)
                )
            )
        }
        return users.toTypedArray()
    }

    /**
     * Update a user.
     */
    override fun update(entity: User) {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.UPDATE_USER,
            entity.username,
            entity.isAdmin,
            entity.isBlocked,
            entity.email
        )
        if (ps.executeUpdate() == 0) {
            throw SQLIntegrityConstraintViolationException("no rows affected")
        }
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
