package social.friendship.application

import org.apache.logging.log4j.LogManager
import social.common.ddd.Service
import social.common.events.FriendshipRemoved
import social.common.events.FriendshipRequestAccepted
import social.common.events.FriendshipRequestRejected
import social.common.events.FriendshipRequestSent
import social.common.events.MessageReceived
import social.common.events.MessageSent
import social.friendship.domain.Friendship
import social.friendship.domain.Friendship.FriendshipID
import social.friendship.domain.FriendshipRequest
import social.friendship.domain.FriendshipRequest.FriendshipRequestID
import social.friendship.domain.Message
import social.friendship.domain.Message.MessageID
import social.friendship.domain.User
import social.friendship.social.friendship.application.DatabaseCredentials
import java.nio.file.Files
import java.nio.file.Paths

interface FriendshipService : FriendshipProcessor, FriendshipRequestProcessor, MessageProcessor, UserProcessor, Service

/**
 * FriendshipServiceVerticle is the main entry point for the Friendship Service.
 * It is responsible for handling all the business logic related to friendships, friendship requests, messages and users.
 * It also deploys the KafkaFriendshipProducerVerticle to publish events to the Kafka broker.
 * @param userRepository The repository to interact with the User entity.
 * @param friendshipRepository The repository to interact with the Friendship entity.
 * @param friendshipRequestRepository The repository to interact with the FriendshipRequest entity.
 * @param messageRepository The repository to interact with the Message entity.
 * @param credentials The database credentials to connect to the MySQL database.
 * @param shouldConnectToDB A flag to determine if the service should connect to the database, used for testing purposes.
 */
class FriendshipServiceImpl(
    private val userRepository: UserRepository,
    private val friendshipRepository: FriendshipRepository,
    private val friendshipRequestRepository: FriendshipRequestRepository,
    private val messageRepository: MessageRepository,
    private val kafkaProducer: KafkaProducerVerticle,
    private val credentials: DatabaseCredentials? = null,
    shouldConnectToDB: Boolean? = true,
) : FriendshipService {
    private val logger = LogManager.getLogger(this::class)

    init {
        if (shouldConnectToDB == true) connectToDatabase()
    }

    /**
     * Connects to the MySQL database using the provided credentials or the default credentials given by the environment
     * variables.
     */
    private fun connectToDatabase() {
        credentials?.let {
            connectToDatabaseWith(it)
        } ?: connectToDatabaseWithDefaultCredentials()
    }

    /**
     * Connects to the MySQL database using the provided credentials.
     * @param credentials The database credentials to connect to the MySQL database.
     */
    private fun connectToDatabaseWith(credentials: DatabaseCredentials) {
        listOf(userRepository, friendshipRepository, friendshipRequestRepository, messageRepository).forEach {
            it.connect(credentials.host, credentials.port, credentials.dbName, credentials.username, credentials.password)
        }
    }

    /**
     * Connects to the MySQL database using the provided credentials.
     * @param host The host of the MySQL database.
     * @param port The port of the MySQL database.
     * @param dbName The name of the MySQL database.
     * @param username The username to connect to the MySQL database.
     * @param password The password to connect to the MySQL database.
     */
    private fun connectToDatabaseWith(host: String, port: String, dbName: String, username: String, password: String) {
        listOf(userRepository, friendshipRepository, friendshipRequestRepository, messageRepository).forEach {
            it.connect(host, port, dbName, username, password)
        }
    }

    /**
     * Connects to the MySQL database using the default credentials given by the environment variables.
     */
    private fun connectToDatabaseWithDefaultCredentials() {
        val host = System.getenv("DB_HOST")
        val port = System.getenv("DB_PORT")
        val dbName = System.getenv("MYSQL_DATABASE")
        val username = System.getenv("MYSQL_USER")
        val password = System.getenv("MYSQL_PASSWORD")
            ?: Files.readString(Paths.get("/run/secrets/db_password")).trim()

        connectToDatabaseWith(host, port, dbName, username, password)
    }

    /**
     * Stops the FriendshipServiceVerticle and closes the database connections.
     */
    override fun addFriendship(friendship: Friendship) = friendshipRepository.save(friendship)

    /**
     * Retrieves a friendship by its ID.
     * @param friendshipID The ID of the friendship to retrieve.
     * @return The friendship with the given ID, or null if it does not exist.
     */
    override fun getFriendship(friendshipID: FriendshipID): Friendship? = friendshipRepository.findById(friendshipID)

    /**
     * Deletes a friendship by its ID. Also publishes a FriendshipRemoved event to the Kafka broker and the Vert.x event
     * bus.
     * @param friendshipID The ID of the friendship to delete.
     * @return The deleted friendship, or null if it does not exist.
     */
    override fun deleteFriendship(friendshipID: FriendshipID): Friendship {
        return friendshipRepository.deleteById(friendshipID)?.also {
            val event = FriendshipRemoved(it.user1.id.value, it.user2.id.value)
            kafkaProducer.publishEvent(event)
        } ?: throw IllegalArgumentException("Friendship not found")
    }

    /**
     * Retrieves all friendships.
     * @return An array containing all the friendships.
     */
    override fun getAllFriendships(): Array<Friendship> = friendshipRepository.findAll()

    /**
     * Retrieves all friendships of a user.
     * @param userID The ID of the user whose friendships to retrieve.
     * @return An iterable containing all the friendships of the user.
     */
    override fun getAllFriendsByUserId(userID: User.UserID): Iterable<User> = friendshipRepository.findAllFriendsOf(userID)

    /**
     * Adds a friendship request.
     * @param friendshipRequest The friendship request to add.
     */
    override fun addFriendshipRequest(friendshipRequest: FriendshipRequest) {
        friendshipRequestRepository.save(friendshipRequest)
        val event = FriendshipRequestSent(
            sender = friendshipRequest.from.id.value,
            receiver = friendshipRequest.to.id.value,
        )
        kafkaProducer.publishEvent(event)
    }

    /**
     * Retrieves a friendship request by its ID.
     * @param friendshipRequestID The ID of the friendship request to retrieve.
     * @return The friendship request with the given ID, or null if it does not exist.
     */
    override fun getFriendshipRequest(friendshipRequestID: FriendshipRequestID): FriendshipRequest? = friendshipRequestRepository.findById(friendshipRequestID)

    /**
     * Rejects a friendship request. Also publishes a FriendshipRequestRejected event to the Kafka broker and the Vert.x
     * event bus.
     * @param friendshipRequest The friendship request to reject.
     * @return The rejected friendship request, or null if it does not exist.
     */
    override fun rejectFriendshipRequest(friendshipRequest: FriendshipRequest): FriendshipRequest {
        return friendshipRequestRepository.deleteById(friendshipRequest.id)?.also {
            val event = FriendshipRequestRejected(
                sender = it.to.id.value,
                receiver = it.from.id.value
            )
            kafkaProducer.publishEvent(event)
        } ?: throw IllegalArgumentException("Friendship request not found")
    }

    /**
     * Retrieves all friendship requests in the repository.
     * @return An array containing all the friendship requests.
     */
    override fun getAllFriendshipRequests(): Array<FriendshipRequest> = friendshipRequestRepository.findAll()

    /**
     * Retrieves all friendship requests of a user.
     * @param userID The ID of the user whose friendship requests to retrieve.
     * @return An iterable containing all the friendship requests of the user.
     */
    override fun getAllFriendshipRequestsByUserId(userID: User.UserID): Iterable<FriendshipRequest> = friendshipRequestRepository.getAllFriendshipRequestsOf(userID)

    /**
     * Accepts a friendship request. Also publishes a FriendshipRequestAccepted event to the Kafka broker and the Vert.x
     * event bus.
     * @param request The friendship request to accept.
     */
    override fun acceptFriendshipRequest(request: FriendshipRequest) {
        friendshipRequestRepository.deleteById(request.id)?.let {
            friendshipRepository.save(Friendship.of(request))
            val event = FriendshipRequestAccepted(
                sender = request.to.id.value,
                receiver = request.from.id.value
            )
            kafkaProducer.publishEvent(event)
        } ?: throw IllegalArgumentException("Friendship request not found")
    }

    /**
     * Adds a message to the repository.
     * @param message The message to add.
     */
    override fun addMessage(message: Message) = messageRepository.save(message)

    /**
     * Handles a received message. Also publishes a MessageReceived event to the Kafka broker and the Vert.x event bus.
     * @param message The received message.
     */
    override fun receivedMessage(message: Message) {
        messageRepository.save(message)
        val event = MessageReceived(
            id = message.id.value.toString(),
            sender = message.sender.id.value,
            receiver = message.receiver.id.value,
            message = message.content,
            timestamp = message.timestamp.toString()
        )
        kafkaProducer.publishEvent(event)
    }

    /**
     * Handles a sent message. Also publishes a MessageSent event to the Kafka broker and the Vert.x event bus.
     * @param message The sent message.
     */
    override fun sentMessage(message: Message) {
        messageRepository.save(message)
        val event = MessageSent(
            id = message.id.value.toString(),
            sender = message.sender.id.value,
            receiver = message.receiver.id.value,
            message = message.content,
            timestamp = message.timestamp.toString()
        )
        kafkaProducer.publishEvent(event)
    }

    /**
     * Retrieves a message by its ID.
     * @param messageID The ID of the message to retrieve.
     * @return The message with the given ID, or null if it does not exist.
     */
    override fun getMessage(messageID: MessageID): Message? = messageRepository.findById(messageID)

    /**
     * Deletes a message by its ID.
     * @param messageID The ID of the message to delete.
     * @return The deleted message, or null if it does not exist.
     */
    override fun deleteMessage(messageID: MessageID): Message? = messageRepository.deleteById(messageID)

    /**
     * Retrieves all messages in the repository.
     * @return An array containing all the messages.
     */
    override fun getAllMessages(): Array<Message> = messageRepository.findAll()

    /**
     * Retrieves all messages sent by a user.
     * @param userID The ID of the user who sent the messages.
     * @return An iterable containing all the messages sent by the user.
     */
    override fun getAllMessagesReceivedByUserId(userID: User.UserID): Iterable<Message> = messageRepository.findAllMessagesReceivedBy(userID)

    /**
     * Retrieves all messages exchanged between 2 users.
     * @param user1Id The ID of the first user.
     * @param user2Id The ID of the second user.
     * @return An iterable containing all the messages received by the user.
     */
    override fun getAllMessagesExchangedBetween(user1Id: User.UserID, user2Id: User.UserID): Iterable<Message> = messageRepository.findAllMessagesExchangedBetween(user1Id, user2Id)

    /**
     * Adds a user to the repository.
     * @param user The user to add.
     */
    override fun addUser(user: User) {
        userRepository.save(user)
    }

    /**
     * Retrieves a user by their ID.
     * @param userID The ID of the user to retrieve.
     * @return The user with the given ID, or null if it does not exist.
     */
    override fun getUser(userID: User.UserID): User? = userRepository.findById(userID)
}
