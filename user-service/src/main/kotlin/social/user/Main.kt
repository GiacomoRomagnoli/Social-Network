package social.user

import io.vertx.core.Vertx
import social.user.application.AuthServiceImpl
import social.user.application.UserServiceImpl
import social.user.infrastructure.controller.event.KafkaUserProducerVerticle
import social.user.infrastructure.controller.rest.HttpServerVerticle
import social.user.infrastructure.persitence.sql.CredentialsSQLRepository
import social.user.infrastructure.persitence.sql.SQLUtils
import social.user.infrastructure.persitence.sql.UserSQLRepository
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    val vertx = Vertx.vertx()

    val repository = UserSQLRepository()
    repository.connect(
        System.getenv("DB_HOST"),
        System.getenv("DB_PORT"),
        System.getenv("MYSQL_DATABASE"),
        System.getenv("MYSQL_USER"),
        Files.readString(Paths.get("/run/secrets/db_password")).trim(),
    )
    val credentialsRepository = CredentialsSQLRepository(
        SQLUtils.mySQLConnection(
            System.getenv("DB_HOST"),
            System.getenv("DB_PORT"),
            System.getenv("MYSQL_DATABASE"),
            System.getenv("MYSQL_USER"),
            Files.readString(Paths.get("/run/secrets/db_password")).trim()
        )
    )

    val producer = KafkaUserProducerVerticle()
    vertx.deployVerticle(producer).onComplete {
        if (it.succeeded()) {
            val userService = UserServiceImpl(repository, producer)
            val authService = AuthServiceImpl(credentialsRepository, producer)
            val server = HttpServerVerticle(userService, authService)
            vertx.deployVerticle(userService)
            vertx.deployVerticle(server)
        } else {
            println("producer deployed with error: ${it.cause().message}")
        }
    }
}
