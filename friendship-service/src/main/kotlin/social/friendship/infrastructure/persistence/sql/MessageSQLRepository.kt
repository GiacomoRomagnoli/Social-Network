package social.friendship.infrastructure.persistence.sql

import social.friendship.application.MessageRepository
import social.friendship.domain.Message
import social.friendship.domain.Message.MessageID
import social.friendship.domain.User
import social.friendship.infrastructure.persistence.sql.SQLUtils.prepareStatement
import java.sql.SQLException
import java.sql.SQLIntegrityConstraintViolationException
import java.sql.Timestamp
import java.util.UUID

/**
 * SQL implementation of the MessageRepository.
 */
class MessageSQLRepository : MessageRepository, AbstractSQLRepository() {

    /**
     * Find a message by its ID.
     * @param id the ID of the message
     * @return the message if found, null otherwise
     */
    override fun findById(id: MessageID): Message? {
        prepareStatement(
            connection,
            SQLOperation.Query.SELECT_MESSAGE_BY_ID,
            id.value.toString()
        ).use { ps ->
            ps.executeQuery().use {
                return if (it.next())
                    Message.of(
                        UUID.fromString(it.getString(SQLColumns.MessageTable.ID)),
                        User.of(it.getString(SQLColumns.MessageTable.SENDER)),
                        User.of(it.getString(SQLColumns.MessageTable.RECEIVER)),
                        it.getString(SQLColumns.MessageTable.CONTENT),
                        it.getTimestamp(SQLColumns.MessageTable.TIMESTAMP).toInstant()
                    )
                else null
            }
        }
    }

    /**
     * Save a message.
     * @param entity the message to save
     */
    override fun save(entity: Message) {
        prepareStatement(
            connection,
            SQLOperation.Update.INSERT_MESSAGE,
            entity.id.value.toString(),
            entity.sender.id.value,
            entity.receiver.id.value,
            entity.content,
            Timestamp.from(entity.timestamp)
        ).use { it.executeUpdate() }
    }

    /**
     * Delete a message by its ID.
     * @param id the ID of the message
     * @return the deleted message if found, null otherwise
     */
    override fun deleteById(id: MessageID): Message? {
        connection.autoCommit = false
        try {
            val messageToDelete = findById(id) ?: return null

            prepareStatement(
                connection,
                SQLOperation.Update.DELETE_MESSAGE_BY_ID,
                id.value.toString()
            ).use {
                return if (it.executeUpdate() > 0) {
                    connection.commit()
                    messageToDelete
                } else {
                    connection.rollback()
                    null
                }
            }
        } catch (e: SQLException) {
            connection.rollback()
            throw e
        } finally {
            connection.autoCommit = true
        }
    }

    /**
     * Find all messages.
     * @return all messages
     */
    override fun findAll(): Array<Message> {
        prepareStatement(
            connection,
            SQLOperation.Query.SELECT_ALL_MESSAGES
        ).use { ps ->
            ps.executeQuery().use {
                val messages = mutableListOf<Message>()
                while (it.next()) {
                    messages.add(
                        Message.of(
                            UUID.fromString(it.getString(SQLColumns.MessageTable.ID)),
                            User.of(it.getString(SQLColumns.MessageTable.SENDER)),
                            User.of(it.getString(SQLColumns.MessageTable.RECEIVER)),
                            it.getString(SQLColumns.MessageTable.CONTENT),
                            it.getTimestamp(SQLColumns.MessageTable.TIMESTAMP).toInstant()
                        )
                    )
                }
                return messages.toTypedArray()
            }
        }
    }

    /**
     * Find all messages received by a user.
     * @param userID the ID of the user
     * @return all messages received by the user
     */
    override fun findAllMessagesReceivedBy(userID: User.UserID): Iterable<Message> {
        prepareStatement(
            connection,
            SQLOperation.Query.SELECT_MESSAGES_RECEIVED_BY_USER,
            userID.value
        ).use { ps ->
            ps.executeQuery().use {
                val messages = mutableListOf<Message>()
                while (it.next()) {
                    messages.add(
                        Message.of(
                            UUID.fromString(it.getString(SQLColumns.MessageTable.ID)),
                            User.of(it.getString(SQLColumns.MessageTable.SENDER)),
                            User.of(it.getString(SQLColumns.MessageTable.RECEIVER)),
                            it.getString(SQLColumns.MessageTable.CONTENT),
                            it.getTimestamp(SQLColumns.MessageTable.TIMESTAMP).toInstant()
                        )
                    )
                }
                return messages
            }
        }
    }

    /**
     * Find all messages exchanged between two users.
     * @param user1 the ID of the first user
     * @param user2 the ID of the second user
     * @return all messages exchanged between the two users
     */
    override fun findAllMessagesExchangedBetween(
        user1: User.UserID,
        user2: User.UserID
    ): Iterable<Message> {
        prepareStatement(
            connection,
            SQLOperation.Query.SELECT_MESSAGES_EXCHANGED_BETWEEN_USERS,
            user1.value,
            user2.value,
            user2.value,
            user1.value
        ).use { ps ->
            ps.executeQuery().use {
                val messages = mutableListOf<Message>()
                while (it.next()) {
                    messages.add(
                        Message.of(
                            UUID.fromString(it.getString(SQLColumns.MessageTable.ID)),
                            User.of(it.getString(SQLColumns.MessageTable.SENDER)),
                            User.of(it.getString(SQLColumns.MessageTable.RECEIVER)),
                            it.getString(SQLColumns.MessageTable.CONTENT),
                            it.getTimestamp(SQLColumns.MessageTable.TIMESTAMP).toInstant()
                        )
                    )
                }
                return messages
            }
        }
    }

    /**
     * Update a message.
     * @param entity the message to update
     */
    override fun update(entity: Message) {
        prepareStatement(
            connection,
            SQLOperation.Update.UPDATE_MESSAGE,
            entity.content,
            entity.id.value.toString(),
        ).use {
            if (it.executeUpdate() == 0) {
                throw SQLIntegrityConstraintViolationException("no rows affected")
            }
        }
    }
}
