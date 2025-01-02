package social.friendship.infrastructure

import java.io.File
import java.util.UUID

abstract class DockerSQLTest : DockerTest() {
    internal val host = "friendship-sql-repository"
    internal val database = "friendship"
    internal val user = "test_user"
    internal val password = "password"
    internal val port = "3306"

    /**
     * Generates the init.sql file for the friendship database in the temporary folder, in order to be used in the
     * docker-compose file, since the file needs to be in the same folder as the docker-compose file.
     *
     * @return the generated init.sql file
     */
    fun generateInitSQLFile(): File {
        return copyFileToTempFolder("social/friendship/infrastructure/persistence/sql/init.sql")
    }

    /**
     * Generates the docker-compose file for the friendship database in the temporary folder
     *
     * @return the generated docker-compose file
     */
    fun generateDockerComposeFile(): File {
        return createDockerComposeFile(
            this::class.java,
            "social/friendship/infrastructure/persistence/sql/docker-compose.yml",
            path = File.createTempFile("docker-compose", ".yml"),
            "ROOT_PASSWORD" to UUID.randomUUID().toString(),
            "DB_NAME" to database,
            "USER" to user,
            "PASSWORD" to password,
            "PORT" to port,
            "INIT_SQL_FILE" to generateInitSQLFile().name
        )
    }
}
