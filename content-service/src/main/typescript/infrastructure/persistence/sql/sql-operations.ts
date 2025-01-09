// Post operation
const INSERT_POST = "INSERT INTO post (author, content, id) VALUES (?, ?, ?)";
const DELETE_POST_BY_ID = "DELETE FROM post WHERE id = ?"
const FIND_POST_BY_ID = "SELECT * FROM (post INNER JOIN user ON post.author = user.email) WHERE id = ?";
const FIND_ALL_POST = "SELECT * FROM (post INNER JOIN user ON post.author = user.email)";
const UPDATE_POST = "UPDATE post SET author = ?, content = ? WHERE id = ?";
const GET_FEED = `
    SELECT post.id, post.author, post.content, user.userName
    FROM post
             INNER JOIN user ON post.author = user.email
             INNER JOIN friendship
                        ON (friendship.user1 = ? AND friendship.user2 = post.author)
                            OR (friendship.user2 = ? AND friendship.user1 = post.author)
`;

// User operations
const INSERT_USER = "INSERT INTO user (userName, email) VALUES (?, ?)";
const DELETE_USER_BY_ID = "DELETE FROM user WHERE email = ?"
const FIND_USER_BY_ID = "SELECT * FROM user WHERE user.email = ?";
const FIND_ALL_USERS = "SELECT * FROM user";
const UPDATE_USER = "UPDATE user SET userName = ? WHERE email = ?";

// Friendship operations
const INSERT_FRIENDSHIP = "INSERT INTO friendship (user1, user2) VALUES (?, ?)";
const DELETE_FRIENDSHIP_BY_ID = "DELETE FROM friendship WHERE user1 = ? AND user2 = ?"
const FIND_ALL_FRIENDSHIP = `
    SELECT 
        friendship.user1,
        u1.userName AS userName1,
        friendship.user2,
        u2.userName AS userName2
    FROM friendship
        INNER JOIN user u1 ON friendship.user1 = u1.email
        INNER JOIN user u2 ON friendship.user2 = u2.email
`;
const FIND_FRIENDSHIP_BY_ID = `
    SELECT 
        friendship.user1,
        u1.userName AS userName1,
        friendship.user2,
        u2.userName AS userName2
    FROM friendship
        INNER JOIN user u1 ON friendship.user1 = u1.email
        INNER JOIN user u2 ON friendship.user2 = u2.email
    WHERE friendship.user1 = ? AND friendship.user2 = ?
`;

export {
    INSERT_POST,
    INSERT_USER,
    FIND_POST_BY_ID,
    FIND_ALL_POST,
    GET_FEED,
    INSERT_FRIENDSHIP,
    FIND_USER_BY_ID,
    FIND_FRIENDSHIP_BY_ID,
    DELETE_POST_BY_ID,
    DELETE_USER_BY_ID,
    DELETE_FRIENDSHIP_BY_ID,
    FIND_ALL_USERS,
    UPDATE_USER,
    FIND_ALL_FRIENDSHIP,
    UPDATE_POST,
}