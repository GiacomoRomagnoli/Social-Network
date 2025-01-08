import {afterEach, beforeEach, describe, expect, test} from '@jest/globals';
import {
    SqlPostRepository,
    SqlUserRepository
} from "../../main/typescript/infrastructure/persistence/sql/sql-repository";
import fs from 'node:fs';
import {postFrom, postOf, userOf} from "../../main/typescript/domain/domain";
import {Connectable, PostRepository, UserRepository} from "../../main/typescript/infrastructure/persistence/repository";

describe("sql-repository module", () => {
    const shell = require("shelljs");
    const password = fs.readFileSync("./db-password.txt", 'utf8');
    let postRepository: PostRepository;
    let userRepository: UserRepository;
    const connect =  async (repository: Connectable) =>
        await repository.connect("127.0.0.1", "content", "user", password);

    beforeEach(() => {
        const result = shell.exec("docker compose up --wait");
        console.log(result);
        postRepository = new SqlPostRepository();
        userRepository = new SqlUserRepository();
    });

  afterEach(() => {
        const result = shell.exec("docker compose down -v");
        console.log(result);
   });

   test("cannot save a post with a non existing author", async () => {
       const post = postFrom("unknown", "unknown@email.com", "should throw");
       await connect(postRepository);
       await expect(postRepository.save(post)).rejects.toThrow();
   });

   test("can save a post if the user exist", async () => {
       const user = userOf("example", "example@email.com");
       const post = postOf(user, "should be saved");
       await connect(postRepository);
       await connect(userRepository);
       await userRepository.save(user);
       await postRepository.save(post);
       const result = await postRepository.findByID(post.id);
       expect(result).toStrictEqual(post);
   });

   test("test can retrieve all posts", async () => {
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

});