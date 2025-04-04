package social.common.ddd

import kotlin.js.JsExport

/**
 * Marker interface to easily identify a domain object as service.
 */
expect interface Service

/**
 * class to identify an object as ID
 */
@JsExport
open class ID<I> (open val value: I) : ValueObject {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ID<*>) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value.toString()
    }
}

/**
 * class to identify a DDD object as Entity
 */
@JsExport
open class Entity<I : ID<*>>(open val id: I) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Entity<*>) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return id.toString()
    }
}

/**
 * Marker interface to easily identify a domain object as value object.
 */
@JsExport
interface ValueObject

/**
 * Marker interface to easily identify a domain object as aggregate root.
 */
@JsExport
open class AggregateRoot<I : ID<*>> (id: I) : Entity<I>(id)

/**
 * Marker interface to easily identify a domain object as repository.
 */
@JsExport
interface Repository<I : ID<*>, E : Entity<*>> {
    fun findById(id: I): E?
    fun save(entity: E)
    fun deleteById(id: I): E?
    fun findAll(): Array<E>
    fun update(entity: E)
}

/**
 * Marker interface to easily identify a domain object as factory.
 */
@JsExport
interface Factory<E : Entity<*>>

/**
 * Marker interface to easily identify a domain object as domain event
 */
@JsExport
interface DomainEvent
