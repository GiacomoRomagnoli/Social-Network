import {afterEach, beforeEach, describe, expect, test} from "@jest/globals";
import {
    SqlFriendshipRepository,
    SqlPostRepository,
    SqlUserRepository
} from "../../../main/typescript/infrastructure/persistence/sql/sql-repository";
import {ContentService, ContentServiceImpl} from "../../../main/typescript/application/service";
import {DefaultMiddlewares, Server} from "../../../main/typescript/infrastructure/api";
import {getRouter} from "../../../main/typescript/infrastructure/router";
import {friendshipOf, postOf, UserID, userOf} from "../../../main/typescript/domain/domain";
import {social} from "../../../main/typescript/commons-lib";
import StatusCode = social.common.endpoint.StatusCode;

describe("api module", () => {
    const shell = require("shelljs");
    let service: ContentService
    let server: Server

    beforeEach(async () => {
        const result = shell.exec("docker compose up --wait");
        console.log(result);
        service = new ContentServiceImpl(
            new SqlFriendshipRepository(),
            new SqlPostRepository(),
            new SqlUserRepository()
        );
        await service.init(3306);
        server = new Server(8080, DefaultMiddlewares, getRouter(service));
        return server.start().then(() => console.log("server up"));
    }, 60 * 1000);

    afterEach(async () => {
        await server.stop();
        const result = shell.exec("docker compose down -v");
        console.log(result);
    });

    test("publish a post successfully", async () => {
        const user = userOf("John Doe", "johndoe@example.com");
        const content = "My first post!";
        await service.addUser(user);
        const response = await fetch("http://localhost:8080/contents/posts", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                user: {
                    name: user.userName,
                    email: user.email,
                },
                content: content,
            }),
        });
        const result = await service.getPostByAuthor(user.id);
        expect(response.status).toBe(StatusCode.OK);
        expect(result.length).toBe(1);
        expect(result[0].author).toStrictEqual(user);
        expect(result[0].content).toBe(content);
    });

    test("publish a post malformed", async () => {
        const user = userOf("John Doe", "johndoe@example.com");
        const content = "My first post!";
        await service.addUser(user);
        const response = await fetch("http://localhost:8080/contents/posts", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                name: user.userName,
                email: user.email,
                content: content,
            }),
        });
        const result = await service.getPostByAuthor(user.id);
        expect(response.status).toBe(StatusCode.BAD_REQUEST);
        expect(result.length).toBe(0);
    });

    test("publish a post by an unregistered user", async () => {
        const response = await fetch("http://localhost:8080/contents/posts", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                user: {
                    name: "John Doe",
                    email: "johndoe@example.com",
                },
                content: "My first post!",
            }),
        });
        const result = await service.getPostByAuthor(new UserID("johndoe@example.com"));
        expect(response.status).toBe(StatusCode.FORBIDDEN);
        expect(result.length).toBe(0);
    });

    test("get all posts by author", async () => {
        const user1 = userOf("John Doe", "johndoe@example.com");
        const user2 = userOf("user", "user@example.com");
        const content = "to be retrieved";
        const post1 = postOf(user1, content);
        const post2 = postOf(user1, content);
        const post3 = postOf(user2, "not to be retrieved");
        await service.addUser(user1);
        await service.addUser(user2);
        await service.addPost(post1);
        await service.addPost(post2);
        await service.addPost(post3);
        const response = await fetch("http://localhost:8080/contents/posts/johndoe@example.com");
        const result = await response.json();
        expect(response.status).toBe(StatusCode.OK);
        expect(result.length).toBe(2);
        expect(result[0].id === post1.id.id || result[0].id === post2.id.id).toBe(true);
        expect(result[0].author.name).toBe(user1.userName);
        expect(result[0].author.email).toBe(user1.email);
        expect(result[0].content).toBe(content);
        expect(result[1].id === post1.id.id || result[1].id === post2.id.id).toBe(true);
        expect(result[1].author.name).toBe(user1.userName);
        expect(result[1].author.email).toBe(user1.email);
        expect(result[1].content).toBe(content);
    });

    test("delete a post successfully", async () => {
        const user = userOf("John Doe", "johndoe@example.com");
        const post = postOf(user, "to be deleted");
        await service.addUser(user);
        await service.addPost(post);
        const response = await fetch(`http://localhost:8080/contents/posts/${user.email}/${post.id.id}`, {
            method: "DELETE",
        });
        const body = await response.json();
        const result = await service.getPost(post.id)
        expect(response.status).toBe(StatusCode.OK);
        expect(body).toMatchObject({
            id: post.id.id,
            author: {
                name: user.userName,
                email: user.email,
            },
            content: post.content,
        });
        expect(result).toBeUndefined();
    })

    test("delete a post without ownership", async () => {
        const user = userOf("John Doe", "johndoe@example.com");
        const post = postOf(user, "to be deleted");
        await service.addUser(user);
        await service.addPost(post);
        const response = await fetch(`http://localhost:8080/contents/posts/bob@example.com/${post.id.id}`, {
            method: "DELETE",
        });
        const result = await service.getPost(post.id)
        expect(response.status).toBe(StatusCode.FORBIDDEN);
        expect(result).toStrictEqual(post);
    });

    test("get feed successfully", async () => {
        const john = userOf("John Doe", "johndoe@example.com");
        const bob = userOf("Bob Doe", "bobdoe@example.com");
        const alice = userOf("Alice Doe", "alicedoe@example.com");
        const bobPost = postOf(bob, "this is bob post");
        const alicePost = postOf(alice, "this is alice post");
        await service.addUser(john);
        await service.addUser(bob);
        await service.addUser(alice);
        await service.addPost(bobPost);
        await service.addPost(alicePost);
        await service.addFriendship(friendshipOf(john, bob));
        await service.addFriendship(friendshipOf(john, alice));
        const response = await fetch(`http://localhost:8080/contents/posts/feed/${john.email}`);
        const result = await response.json();
        expect(response.status).toBe(StatusCode.OK);
        expect(result.posts.length).toBe(2);
        expect(result.owner).toMatchObject({
            name: john.userName,
            email: john.email,
        });
        expect(result.posts[0].id === alicePost.id.id || result.posts[0].id === bobPost.id.id).toBe(true);
        expect(result.posts[0].content === alicePost.content || result.posts[0].content === bobPost.content).toBe(true);
        expect(result.posts[1].id === alicePost.id.id || result.posts[1].id === bobPost.id.id).toBe(true);
        expect(result.posts[1].content === alicePost.content || result.posts[1].content === bobPost.content).toBe(true);
    });

    test("get feed filtered successfully", async () => {
        const john = userOf("John Doe", "johndoe@example.com");
        const bob = userOf("Bob Doe", "bobdoe@example.com");
        const alice = userOf("Alice Doe", "alicedoe@example.com");
        const bobPost = postOf(bob, "this is bob post");
        const alicePost = postOf(alice, "this is alice post");
        await service.addUser(john);
        await service.addUser(bob);
        await service.addUser(alice);
        await service.addPost(bobPost);
        await service.addPost(alicePost);
        await service.addFriendship(friendshipOf(john, bob));
        await service.addFriendship(friendshipOf(john, alice));
        const response = await fetch(`http://localhost:8080/contents/posts/feed/${john.email}?keyword=bob`);
        const result = await response.json();
        expect(response.status).toBe(StatusCode.OK);
        expect(result.posts.length).toBe(1);
        expect(result.posts[0]).toMatchObject({
            id: bobPost.id.id,
            author: {
                name: bob.userName,
                email: bob.email,
            },
            content: bobPost.content,
        });
    });

});