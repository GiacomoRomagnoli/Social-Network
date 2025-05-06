[« Back to Index](../docs.md)
# DDD
The project is based on the principles of Domain-Driven Design (DDD). 
We have defined the following abstractions to model the tactical patterns of DDD:

## ID
```kotlin
open class ID<I> (open val id: I) : ValueObject {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ID<*>) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return this.id.hashCode()
    }

    override fun toString(): String {
        return id.toString()
    }
}
```
Class that represents the identity of an entity.

## Entity
```kotlin
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
```
Class to identify a DDD object as Entity.

## Value Object
```kotlin
interface ValueObject
```
Marker interface to easily identify a domain object as value object.

## Aggregate Root
```kotlin
open class AggregateRoot<I : ID<*>> (id: I) : Entity<I>(id)
```
Class to easily identify a domain object as aggregate root, that is a specialization of entity.

## Repository
```kotlin
interface Repository<I : ID<*>, E : Entity<*>> {
    fun findById(id: I): E?
    fun save(entity: E)
    fun deleteById(id: I): E?
    fun findAll(): Array<E>
    fun update(entity: E)
}
```
Interface to easily identify a domain object as repository, with minimal methods.

## Factory
```kotlin
interface Factory<E : Entity<*>>
```
Marker interface to easily identify a domain object as factory.

## Event
```kotlin
interface DomainEvent
```
Marker interface to easily identify a domain object as domain event.

## Service
```kotlin
interface Service
```
Marker interface to easily identify a domain object as service.

[« Back to Index](../docs.md) | [« Previous](./incremental-development.md) | [Next »](./ddd.md)