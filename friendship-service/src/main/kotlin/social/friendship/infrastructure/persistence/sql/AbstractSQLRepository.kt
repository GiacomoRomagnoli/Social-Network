package social.friendship.social.friendship.infrastructure.persistence.sql

import org.apache.logging.log4j.LogManager
import java.sql.Connection

abstract class AbstractSQLRepository {
    protected lateinit var connection: Connection
    private val logger = LogManager.getLogger(this::class)

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

    fun close() {
        logger.trace("Closing connection to database")
        connection.close()
    }
}
