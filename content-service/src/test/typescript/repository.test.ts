import {afterEach, beforeEach, describe, expect, test} from '@jest/globals';
import {
    SqlFriendshipRepository,
    SqlPostRepository,
    SqlUserRepository
} from "../../main/typescript/infrastructure/persistence/sql/sql-repository";
import {friendshipOf, postFrom, postOf, userOf} from "../../main/typescript/domain/domain";
import {
    Connectable,
    FriendshipRepository, getConfiguration,
    PostRepository,
    UserRepository
} from "../../main/typescript/application/repository";
import {NoReferencedRowError} from "../../main/typescript/infrastructure/persistence/sql/sql-errors";

describe("sql-repository module", () => {
    const shell = require("shelljs");
    let postRepository: PostRepository;
    let userRepository: UserRepository;
    let friendshipRepository: FriendshipRepository;
    const connect =  async (repository: Connectable) =>
        await repository.connect(getConfiguration());

    beforeEach(() => {
        const result = shell.exec("docker compose up --wait");
        console.log(result);
        postRepository = new SqlPostRepository();
        userRepository = new SqlUserRepository();
        friendshipRepository = new SqlFriendshipRepository();
    });

    afterEach(() => {
        const result = shell.exec("docker compose down -v");
        console.log(result);
    });

    test("use a repository without connection", async () =>{
        const post = postFrom("unknown", "unknown@email.com", "should throw");
        await expect(postRepository.save(post)).rejects.toThrow(TypeError);
    });

    test("save a post with a non existing author", async () => {
       const post = postFrom("unknown", "unknown@email.com", "should throw");
       await connect(postRepository);
       await expect(postRepository.save(post)).rejects.toThrow(NoReferencedRowError);
   });

    test("save a friendship with non existing users", async () => {
       const friend1 = userOf("friend1", "friend1@email.com");
       const friend2 = userOf("friend2", "friend2@email.com");
       const friendship = friendshipOf(friend1, friend2);
       await connect(friendshipRepository);
       await expect(friendshipRepository.save(friendship)).rejects.toThrow(NoReferencedRowError);
   });

   test("find a post by id", async () => {
       const user = userOf("example", "example@email.com");
       const post = postOf(user, "should be saved");
       await connect(postRepository);
       await connect(userRepository);
       await userRepository.save(user);
       await postRepository.save(post);
       const result = await postRepository.findByID(post.id);
       expect(result).toStrictEqual(post);
   });

   test("retrieve all posts", async () => {
       const user = userOf("example", "example@email.com");
       const post1 = postOf(user, "first");
       const post2 = postOf(user, "second");
       await connect(postRepository);
       await connect(userRepository);
       await userRepository.save(user);
       await postRepository.save(post1);
       await postRepository.save(post2);
       const result = await postRepository.findAll();
       expect(result.length).toBe(2);
       expect(result[0].equals(post1) || result[0].equals(post2)).toBe(true);
       expect(result[1].equals(post1) || result[1].equals(post2)).toBe(true);
   });

   test("retrieve a user feed", async () => {
       const user = userOf("username", "username@email.com");
       const friend1 = userOf("friend1", "friend1@email.com");
       const friend2 = userOf("friend2", "friend2@email.com");
       const friendship1 = friendshipOf(user, friend1);
       const friendship2 = friendshipOf(friend2, user);
       const post1 = postOf(user, "it not belongs to feed");
       const post2 = postOf(friend1, "it belongs to feed");
       const post3 = postOf(friend2, "it belongs to feed");
       await connect(userRepository);
       await connect(postRepository);
       await connect(friendshipRepository);
       await userRepository.save(user);
       await userRepository.save(friend1);
       await userRepository.save(friend2);
       await friendshipRepository.save(friendship1);
       await friendshipRepository.save(friendship2);
       await postRepository.save(post1);
       await postRepository.save(post2);
       await postRepository.save(post3);
       const result = await postRepository.getFeed(user);
       expect(result.posts.length).toBe(2);
       expect(result.owner).toStrictEqual(user);
       expect(result.posts[0].equals(post2) || result.posts[0].equals(post3)).toBe(true);
       expect(result.posts[1].equals(post2) || result.posts[1].equals(post3)).toBe(true);
   });

   test("find a friendship by id", async () => {
       const friend1 = userOf("friend1", "friend1@email.com");
       const friend2 = userOf("friend2", "friend2@email.com");
       const friendship = friendshipOf(friend1, friend2);
       await connect(userRepository);
       await connect(friendshipRepository);
       await userRepository.save(friend1);
       await userRepository.save(friend2);
       await friendshipRepository.save(friendship);
       const result = await friendshipRepository.findByID(friendship.id);
       expect(result).toStrictEqual(friendship);
    });

   test("find a user by id", async () => {
       const user = userOf("username", "username@email.com");
       await connect(userRepository);
       await userRepository.save(user);
       const result = await userRepository.findByID(user.id);
       expect(result).toStrictEqual(user);
   });

   test("delete a user by id", async () => {
       const user = userOf("username", "username@email.com");
       await connect(userRepository);
       await userRepository.save(user);
       const result1 = await userRepository.deleteById(user.id);
       const result2 = await userRepository.findByID(user.id);
       expect(result1).toStrictEqual(user);
       expect(result2).toBeUndefined();
   });

   test("if user doesn't exist retrieves undefined", async () => {
       const user = userOf("username", "username@email.com");
       await connect(userRepository);
       const result = await userRepository.deleteById(user.id);
       expect(result).toBeUndefined();
   });

   test("delete on cascade when a user has been deleted", async () => {
       const friend1 = userOf("friend1", "friend1@email.com");
       const friend2 = userOf("friend2", "friend2@email.com");
       const friendship = friendshipOf(friend1, friend2);
       const post = postOf(friend1, "this will be deleted");
       await connect(userRepository);
       await connect(postRepository);
       await connect(friendshipRepository);
       await userRepository.save(friend1);
       await userRepository.save(friend2);
       await friendshipRepository.save(friendship);
       await postRepository.save(post);
       await userRepository.deleteById(friend1.id);
       const result1 = await friendshipRepository.findByID(friendship.id);
       const result2 = await postRepository.findByID(post.id);
       expect(result1).toBeUndefined();
       expect(result2).toBeUndefined();
   });

   test("update a post", async () => {
       const user1 = userOf("example1", "example1@email.com");
       const user2 = userOf("example2", "example2@email.com");
       const post = postOf(user1, "this will be updated");
       const updatedPost = postOf(user2, "this is updated", post.id.id);
       await connect(userRepository);
       await connect(postRepository);
       await userRepository.save(user1);
       await userRepository.save(user2);
       await postRepository.save(post);
       await postRepository.update(updatedPost);
       const result = await postRepository.findByID(post.id);
       expect(result).toStrictEqual(updatedPost);
   });

});