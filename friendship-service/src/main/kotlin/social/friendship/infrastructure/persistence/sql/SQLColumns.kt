package social.friendship.social.friendship.infrastructure.persistence.sql

object SQLColumns {
    object FriendshipTable {
        const val USER_1 = "user1"
        const val USER_2 = "user2"
    }

    object FriendshipRequestTable {
        const val TO = "user_to"
        const val FROM = "user_from"
    }

    object MessageTable {
        const val ID = "id"
        const val SENDER = "sender"
        const val RECEIVER = "receiver"
        const val CONTENT = "content"
    }

    object UserTable {
        const val ID = "id"
    }
}
