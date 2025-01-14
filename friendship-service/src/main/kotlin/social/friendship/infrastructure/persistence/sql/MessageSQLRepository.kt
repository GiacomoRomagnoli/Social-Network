package social.friendship.social.friendship.infrastructure.persistence.sql

import social.friendship.domain.Message
import social.friendship.domain.Message.MessageID
import social.friendship.social.friendship.domain.User
import social.friendship.social.friendship.infrastructure.persistence.MessageRepository
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.SQLIntegrityConstraintViolationException
import java.util.UUID

class MessageSQLRepository : MessageRepository, AbstractSQLRepository() {

    override fun findById(id: MessageID): Message? {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Query.SELECT_MESSAGE_BY_ID,
            id.value.toString()
        )
        val result = ps.executeQuery()
        return if (result.next()) {
            Message.of(
                UUID.fromString(result.getString(SQLColumns.MessageTable.ID)),
                User.of(result.getString(SQLColumns.MessageTable.SENDER)),
                User.of(result.getString(SQLColumns.MessageTable.RECEIVER)),
                result.getString(SQLColumns.MessageTable.CONTENT)
            )
        } else {
            null
        }
    }

    override fun save(entity: Message) {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Update.INSERT_MESSAGE,
            entity.id.value.toString(),
            entity.sender.id.value,
            entity.receiver.id.value,
            entity.content
        )
        ps.executeUpdate()
    }

    override fun deleteById(id: MessageID): Message? {
        connection.autoCommit = false
        try {
            val messageToDelete = findById(id)
            if (messageToDelete == null) {
                return null
            }

            val ps: PreparedStatement = SQLUtils.prepareStatement(
                connection,
                SQLOperation.Update.DELETE_MESSAGE_BY_ID,
                id.value.toString()
            )

            val result = ps.executeUpdate()

            return if (result > 0) {
                connection.commit()
                messageToDelete
            } else {
                connection.rollback()
                null
            }
        } catch (e: SQLException) {
            connection.rollback()
            throw e
        } finally {
            connection.autoCommit = true
        }
    }

    override fun findAll(): Array<Message> {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Query.SELECT_ALL_MESSAGES
        )
        val result = ps.executeQuery()
        val messages = mutableListOf<Message>()
        while (result.next()) {
            messages.add(
                Message.of(
                    UUID.fromString(result.getString(SQLColumns.MessageTable.ID)),
                    User.of(result.getString(SQLColumns.MessageTable.SENDER)),
                    User.of(result.getString(SQLColumns.MessageTable.RECEIVER)),
                    result.getString(SQLColumns.MessageTable.CONTENT)
                )
            )
        }
        return messages.toTypedArray()
    }

    override fun update(entity: Message) {
        val ps: PreparedStatement = SQLUtils.prepareStatement(
            connection,
            SQLOperation.Update.UPDATE_MESSAGE,
            entity.content,
            entity.id.value.toString(),
        )
        if (ps.executeUpdate() == 0) {
            throw SQLIntegrityConstraintViolationException("no rows affected")
        }
    }
}
