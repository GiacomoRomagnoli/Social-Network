package social.friendship.infrastructure.persistence.sql

import com.mysql.cj.jdbc.exceptions.CommunicationsException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

object SQLUtils {
    private val logger = org.apache.logging.log4j.LogManager.getLogger(SQLUtils::class)

    fun prepareStatement(connection: Connection, sqlStatement: String, vararg params: Any): PreparedStatement {
        val ps = connection.prepareStatement(sqlStatement)
        params.forEachIndexed { index, param ->
            ps.setObject(index + 1, param)
        }
        return ps
    }

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
