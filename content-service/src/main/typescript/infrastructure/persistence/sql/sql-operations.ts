// Post operation
const INSERT_POST = "INSERT INTO post (author, content, id) VALUES (?, ?, ?)"
const FIND_POST_BY_ID = "SELECT * FROM (post INNER JOIN user ON post.author = user.email) WHERE id = ?"
const FIND_ALL_POST = "SELECT * FROM (post INNER JOIN user ON post.author = user.email)"

// User operations
const INSERT_USER = "INSERT INTO user (userName, email) VALUES (?, ?)"

export {INSERT_POST, INSERT_USER, FIND_POST_BY_ID, FIND_ALL_POST}