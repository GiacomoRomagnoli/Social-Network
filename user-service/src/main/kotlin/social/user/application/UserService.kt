package social.user.application

import social.common.ddd.Service
import social.common.events.UserCreated
import social.user.domain.User
import social.user.domain.UserID

/**
 * Interface to represent a service that manages users.
 */
interface UserService : Service {
    fun addUser(user: User)
    fun getUser(id: UserID): User?
    fun renameUser(id: UserID, username: String)
    fun deleteUser(id: UserID): User?
    fun blockUser(id: UserID)
    fun unblockUser(id: UserID)
}

/**
 * Class to represent a user service.
 * @param repository the repository to manage users
 * @param kafkaProducer the Kafka producer verticle
 */
class UserServiceImpl(
    private val repository: UserRepository,
    private val kafkaProducer: KafkaProducerVerticle
) : UserService {

    override fun addUser(user: User) {
        repository.save(user).let {
            kafkaProducer.publishEvent(UserCreated(user.username, user.email))
        }
    }

    override fun getUser(id: UserID): User? = repository.findById(id)

    override fun renameUser(id: UserID, username: String) {
        val user = repository.findById(id)?.rename(username)
            ?: throw IllegalArgumentException("user $id does not exist")
        repository.update(user)
    }

    override fun blockUser(id: UserID) {
        val user = repository.findById(id)?.block()
            ?: throw IllegalArgumentException("user $id does not exist")
        repository.update(user)
    }

    override fun unblockUser(id: UserID) {
        val user = repository.findById(id)?.unblock()
            ?: throw IllegalArgumentException("user $id does not exist")
        repository.update(user)
    }

    override fun deleteUser(id: UserID) = repository.deleteById(id)
}
