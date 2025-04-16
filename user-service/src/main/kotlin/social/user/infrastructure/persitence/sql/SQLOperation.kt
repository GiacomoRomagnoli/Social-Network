package social.user.infrastructure.persitence.sql

/**
 * Object to store the SQL operations.
 */
object SQLOperation {
    const val USER_COUNT =
        """
            SELECT COUNT(*) FROM user
        """
    const val INSERT_USER =
        """
        INSERT INTO user (email, username, admin, blocked)
        VALUES (?, ?, ?, ?)
        """

    const val DELETE_USER_BY_ID =
        """
        DELETE FROM user
        WHERE email = ?
        """

    const val UPDATE_USER =
        """
        UPDATE user
        SET username = ?, admin = ?, blocked = ?
        WHERE email = ?
        """

    const val SELECT_USER_BY_ID =
        """
        SELECT * FROM user
        WHERE email = ?
        """

    const val SELECT_ALL_USERS =
        """
        SELECT * FROM user
        """

    const val INSERT_CREDENTIALS =
        """
            INSERT INTO credentials (user_id, password)
            VALUES (?, ?)
        """

    const val SELECT_CREDENTIALS_BY_ID =
        """
            SELECT * FROM credentials
            WHERE user_id = ?
        """

    const val DELETE_CREDENTIALS_BY_ID =
        """
            DELETE FROM credentials
            WHERE user_id = ?
        """

    const val SELECT_ALL_CREDENTIALS =
        """
            SELECT * FROM credentials
        """

    const val UPDATE_CREDENTIALS =
        """
            UPDATE credentials
            SET password = ?
            WHERE user_id = ?
        """
}
