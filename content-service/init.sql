CREATE TABLE user
(
    userName VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    PRIMARY KEY (email)
);

CREATE TABLE post
(
    id VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    FOREIGN KEY (author) REFERENCES user(email),
    PRIMARY KEY (id)
);

CREATE TABLE friendship
(
    user1 VARCHAR(255) NOT NULL,
    user2 VARCHAR(255) NOT NULL,
    FOREIGN KEY (user1) REFERENCES user(email),
    FOREIGN KEY (user2) REFERENCES user(email),
    PRIMARY KEY (user1, user2)
)
