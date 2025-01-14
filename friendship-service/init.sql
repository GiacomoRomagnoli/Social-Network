CREATE TABLE user (
    id VARCHAR(255) NOT NULL PRIMARY KEY
);

CREATE TABLE friendship_request (
    user_to VARCHAR(255) NOT NULL,
    user_from VARCHAR(255) NOT NULL,
    FOREIGN KEY (user_to) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (user_from) REFERENCES user(id) ON DELETE CASCADE,
    PRIMARY KEY (user_to, user_from)
);

CREATE TABLE friendship (
    user1 VARCHAR(255) NOT NULL,
    user2 VARCHAR(255) NOT NULL,
    FOREIGN KEY (user1) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (user2) REFERENCES user(id) ON DELETE CASCADE,
    PRIMARY KEY (user1, user2)
);

CREATE TABLE message
(
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    user_to VARCHAR(255) NOT NULL,
    user_from VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    FOREIGN KEY (user_to, user_from) REFERENCES friendship(user_to, user_from) ON DELETE CASCADE
);
