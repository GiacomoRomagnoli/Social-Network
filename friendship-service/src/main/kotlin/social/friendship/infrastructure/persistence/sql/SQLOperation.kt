package social.friendship.social.friendship.infrastructure.persistence.sql

object SQLOperation {
    object Query {
        const val SELECT_FRIENDSHIP_BY_ID =
            """
            SELECT * FROM friendship
            WHERE user1 = ? AND user2 = ?
            """
        const val SELECT_ALL_FRIENDSHIPS =
            """
            SELECT * FROM friendship
            """
        const val SELECT_FRIENDSHIP_REQUEST_BY_ID =
            """
            SELECT * FROM friendship_request
            WHERE user_to = ? AND user_from = ?
            """
        const val SELECT_ALL_FRIENDSHIP_REQUESTS =
            """
            SELECT * FROM friendship_request
            """
        const val SELECT_MESSAGE_BY_ID =
            """
            SELECT * FROM message
            WHERE id = ?
            """
        const val SELECT_ALL_MESSAGES =
            """
            SELECT * FROM message
            """
        const val SELECT_USER_BY_ID =
            """
            SELECT * FROM user
            WHERE id = ?
            """
        const val SELECT_ALL_USERS =
            """
            SELECT * FROM user
            """
    }

    object Update {
        const val INSERT_FRIENDSHIP =
            """
            INSERT INTO friendship (user1, user2)
            VALUES (?, ?)
            """
        const val DELETE_FRIENDSHIP_BY_ID =
            """
            DELETE FROM friendship
            WHERE user1 = ? AND user2 = ?
            """
        const val UPDATE_FRIENDSHIP =
            """
            UPDATE friendship
            SET user1 = ?, user2 = ?
            WHERE user1 = ? AND user2 = ?
            """
        const val INSERT_FRIENDSHIP_REQUEST =
            """
            INSERT INTO friendship_request (user_to, user_from)
            VALUES (?, ?)
            """
        const val DELETE_FRIENDSHIP_REQUEST_BY_ID =
            """
            DELETE FROM friendship_request
            WHERE user_to = ? AND user_from = ?
            """
        const val UPDATE_FRIENDSHIP_REQUEST =
            """
            UPDATE friendship_request
            SET user_to = ?, user_from = ?
            WHERE user_to = ? AND user_from = ?
            """
        const val INSERT_MESSAGE =
            """
            INSERT INTO message (id, sender, receiver, content)
            VALUES (?, ?, ?, ?)
            """
        const val DELETE_MESSAGE_BY_ID =
            """
            DELETE FROM message
            WHERE id = ?
            """
        const val UPDATE_MESSAGE =
            """
            UPDATE message
            SET content = ?
            WHERE id = ?
            """
        const val INSERT_USER =
            """
            INSERT INTO user (id)
            VALUES (?)
            """
        const val DELETE_USER_BY_ID =
            """
            DELETE FROM user
            WHERE id = ?
            """
        const val UPDATE_USER =
            """
            UPDATE user
            SET id = ?
            WHERE id = ?
            """
    }
}
