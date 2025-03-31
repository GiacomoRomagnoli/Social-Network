package social.friendship.infrastructure.persistence.sql

/**
 * Object to store the SQL columns.
 */
object SQLColumns {
    /**
     * Object to store the columns of the friendship table.
     */
    object FriendshipTable {
        const val USER_1 = "user1"
        const val USER_2 = "user2"
    }

    /**
     * Object to store the columns of the friendship request table.
     */
    object FriendshipRequestTable {
        const val TO = "user_to"
        const val FROM = "user_from"
    }

    /**
     * Object to store the columns of the message table.
     */
    object MessageTable {
        const val ID = "id"
        const val SENDER = "sender"
        const val RECEIVER = "receiver"
        const val CONTENT = "content"
        const val TIMESTAMP = "timestamp"
    }

    /**
     * Object to store the columns of the user table.
     */
    object UserTable {
        const val ID = "id"
    }
}
