package social.friendship.social.friendship.domain.application

import social.common.ddd.Entity
import social.common.ddd.ID
import social.common.ddd.Repository
import social.common.ddd.Service

interface FriendshipService<I : ID<*>, E : Entity<*>> : Service {
    fun add(entity: E)
    fun getById(id: I): E?
    fun deleteById(id: I): E?
}

class FriendshipServiceImpl<I : ID<*>, E : Entity<*>>(private val repository: Repository<I, E>) : FriendshipService<I, E> {
    override fun add(entity: E) = repository.save(entity)

    override fun getById(id: I): E? = repository.findById(id)

    override fun deleteById(id: I): E? = repository.deleteById(id)
}
