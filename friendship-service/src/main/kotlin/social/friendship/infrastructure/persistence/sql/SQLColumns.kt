package social.friendship.social.friendship.infrastructure.persistence.sql

object SQLColumns {
    object FriendshipTable {
        const val TO = "user_to"
        const val FROM = "user_from"
    }

    object FriendshipRequestTable {
        const val TO = "user_to"
        const val FROM = "user_from"
    }

    object MessageTable {
        const val ID = "id"
        const val TO = "user_to"
        const val FROM = "user_from"
        const val CONTENT = "content"
    }

    object UserTable {
        const val ID = "id"
    }
}
