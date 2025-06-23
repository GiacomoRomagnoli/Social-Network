package social.user

import io.vertx.core.Vertx
import social.user.application.AuthServiceImpl
import social.user.application.CredentialsRepository
import social.user.application.UserRepository
import social.user.application.UserServiceImpl
import social.user.domain.Credentials
import social.user.domain.User
import social.user.infrastructure.controller.event.KafkaUserProducerVerticle
import social.user.infrastructure.controller.rest.AuthApiDecorator
import social.user.infrastructure.persitence.sql.CredentialsSQLRepository
import social.user.infrastructure.persitence.sql.UserSQLRepository
import social.user.infrastructure.probes.ReadinessProbe
import java.nio.file.Files
import java.nio.file.Paths

private fun adminRegistration(
    user: User,
    credentials: Credentials,
    userRepository: UserRepository,
    credentialsRepository: CredentialsRepository
) {
    if (userRepository.findById(user.id) == null) {
        userRepository.save(user)
    }
    if (credentialsRepository.findById(credentials.id) == null) {
        credentialsRepository.save(credentials)
    }
}

fun main(args: Array<String>) {
    val vertx = Vertx.vertx()
    val host = System.getenv("DB_HOST")
    val port = System.getenv("DB_PORT")
    val database = System.getenv("MYSQL_DATABASE")
    val user = System.getenv("MYSQL_USER")
    val password = System.getenv("MYSQL_PASSWORD")
        ?: Files.readString(Paths.get("/run/secrets/db_password")).trim()
    val userRepository = UserSQLRepository()
    userRepository.connect(host, port, database, user, password)
    val credentialsRepository = CredentialsSQLRepository.of(host, port, database, user, password)
    adminRegistration(
        User.of("admin@social.com", "admin", isAdmin = true),
        Credentials.of("admin@social.com", "AdminTest123!"),
        userRepository,
        credentialsRepository
    )
    val producer = KafkaUserProducerVerticle()
    vertx.deployVerticle(producer).onComplete {
        if (it.succeeded()) {
            val userService = UserServiceImpl(userRepository, producer)
            val authService = AuthServiceImpl(credentialsRepository, userRepository, producer)
            val server = AuthApiDecorator(
                userService,
                authService,
                ReadinessProbe(
                    listOf(userRepository, credentialsRepository),
                    listOf(producer)
                )
            )
            vertx.deployVerticle(server)
        } else {
            println("producer deployed with error: ${it.cause().message}")
        }
    }
}
