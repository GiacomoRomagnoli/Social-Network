package social.friendship.social.friendship.domain.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vertx.core.AbstractVerticle
import social.common.ddd.Service
import social.common.events.FriendshipRemoved
import social.common.events.FriendshipRequestAccepted
import social.common.events.FriendshipRequestRejected
import social.common.events.MessageReceived
import social.common.events.MessageSent
import social.common.events.UserCreated
import social.friendship.domain.Friendship
import social.friendship.domain.Friendship.FriendshipID
import social.friendship.domain.FriendshipRequest
import social.friendship.domain.FriendshipRequest.FriendshipRequestID
import social.friendship.domain.Message
import social.friendship.domain.Message.MessageID
import social.friendship.infrastructure.controller.event.KafkaFriendshipProducerVerticle
import social.friendship.social.friendship.domain.User
import social.friendship.social.friendship.infrastructure.persistence.sql.DatabaseCredentials
import social.friendship.social.friendship.infrastructure.persistence.sql.FriendshipRequestSQLRepository
import social.friendship.social.friendship.infrastructure.persistence.sql.FriendshipSQLRepository
import social.friendship.social.friendship.infrastructure.persistence.sql.MessageSQLRepository
import social.friendship.social.friendship.infrastructure.persistence.sql.UserSQLRepository
import java.nio.file.Files
import java.nio.file.Paths

interface FriendshipService : FriendshipProcessor, FriendshipRequestProcessor, MessageProcessor, UserProcessor, Service

interface FriendshipService<I : ID<*>, E : Entity<*>> : Service {
    fun add(entity: E)
    fun getById(id: I): E?
    fun deleteById(id: I): E?
    fun getAll(): Array<E>
}
class FriendshipServiceVerticle(val credentials: DatabaseCredentials? = null, shouldConnectToDB: Boolean? = true) : FriendshipService, AbstractVerticle() {
    private val userRepository = UserSQLRepository()
    private val friendshipRepository = FriendshipSQLRepository()
    private val friendshipRequestRepository = FriendshipRequestSQLRepository()
    private val messageRepository = MessageSQLRepository()
    private val kafkaProducer = KafkaFriendshipProducerVerticle()
    private val mapper: ObjectMapper = jacksonObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
    }

    init {
        if (shouldConnectToDB == true) connectToDatabase()
    }

    fun connectToDatabase() {
        credentials?.let {
            connectToDatabaseWith(it)
        } ?: connectToDatabaseWithDefaultCredentials()
    }

    private fun connectToDatabaseWith(credentials: DatabaseCredentials) {
        listOf(userRepository, friendshipRepository, friendshipRequestRepository, messageRepository).forEach {
            it.connect(credentials.host, credentials.port, credentials.dbName, credentials.username, credentials.password)
        }
    }

    private fun connectToDatabaseWithDefaultCredentials() {
        val host = System.getenv("DB_HOST")
        val port = System.getenv("DB_PORT")
        val dbName = System.getenv("MYSQL_DATABASE")
        val username = System.getenv("MYSQL_USER")
        val password = Files.readString(Paths.get("/run/secrets/db_password")).trim()

        connectToDatabaseWith(DatabaseCredentials(host, port, dbName, username, password))
    }

    override fun addFriendship(friendship: Friendship) = friendshipRepository.save(friendship)

    override fun getFriendship(friendshipID: FriendshipID): Friendship? = friendshipRepository.findById(friendshipID)

    override fun deleteFriendship(friendshipID: FriendshipID): Friendship? {
        return friendshipRepository.deleteById(friendshipID)?.also {
            val event = FriendshipRemoved(it.user1.id.value, it.user2.id.value)
            kafkaProducer.publishEvent(event)
            vertx.eventBus().publish(FriendshipRemoved.TOPIC, mapper.writeValueAsString(it))
        }
    }

    override fun getAllFriendships(): Array<Friendship> = friendshipRepository.findAll()

    override fun getAllFriendsByUserId(userID: User.UserID): Iterable<User> = friendshipRepository.findAllFriendsOf(userID)


class FriendshipServiceImpl<I : ID<*>, E : Entity<*>>(private val repository: Repository<I, E>) : FriendshipService<I, E> {
    override fun add(entity: E) = repository.save(entity)

    override fun getById(id: I): E? = repository.findById(id)

    override fun deleteById(id: I): E? = repository.deleteById(id)

    override fun getAll(): Array<E> = repository.findAll()
}
