package social.friendship.infrastructure.persistence.sql

import org.apache.logging.log4j.LogManager
import social.friendship.application.ConnectableRepository
import social.friendship.infrastructure.probes.SyncProbe
import java.sql.Connection

/**
 * Abstract class to be extended by SQL repositories that need to connect to a database.
 */
abstract class AbstractSQLRepository : ConnectableRepository, SyncProbe {
    protected lateinit var connection: Connection
    private val logger = LogManager.getLogger(this::class)

    /**
     * Connect to a database.
     * @param host the host of the database
     * @param port the port of the database
     * @param dbName the name of the database
     * @param username the username to connect to the database
     * @param password the password to connect to the database
     */
    override fun connect(host: String, port: String, dbName: String, username: String, password: String) {
        logger.trace(
            "Connecting to database with credentials:\n" +
                "host={},\n" +
                "port={},\n" +
                "database={},\n" +
                "username={}",
            host, port, dbName, username
        )
        connection = SQLUtils.mySQLConnection(host, port, dbName, username, password)
    }

    /**
     * Close the connection to the database.
     */
    fun close() {
        logger.trace("Closing connection to database")
        connection.close()
    }

    override fun isReady(): Unit =
        SQLUtils.prepareStatement(connection, "SELECT 1").use { ps ->
            ps.executeQuery().use {
                it.next()
            }
        }
}
