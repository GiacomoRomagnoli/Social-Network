// Post operation
const INSERT_POST = "INSERT INTO post (author, content, id) VALUES (?, ?, ?)";
const FIND_POST_BY_ID = "SELECT * FROM (post INNER JOIN user ON post.author = user.email) WHERE id = ?";
const FIND_ALL_POST = "SELECT * FROM (post INNER JOIN user ON post.author = user.email)";
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

// Friendship operations
const INSERT_FRIENDSHIP = "INSERT INTO friendship (user1, user2) VALUES (?, ?)";

export {INSERT_POST, INSERT_USER, FIND_POST_BY_ID, FIND_ALL_POST, GET_FEED, INSERT_FRIENDSHIP}