import {describe, expect, test} from '@jest/globals';
import {feedOf, postOf, userOf} from "../../main/typescript/domain/domain";

describe("domain module", () => {

    const username = "username";
    const email = "email@your.domain"
    const content = "This is a test"

    test("userOf creates a User", () => {
        const user = userOf(username, email) ;
        expect(user.userName).toBe(username);
        expect(user.id.value).toBe(email)
    });

    test("postOf creates a Post", () => {
        const user = userOf(username, email)
        const post = postOf(user, content)
        expect(post.author).toStrictEqual(user)
        expect(post.content).toBe(content)
    });

    test("feedOf creates a Feed", () => {
        const user = userOf(username, email);
        const post1 = postOf(user, content);
        const post2 = postOf(user, content);
        const posts = [post1, post2];
        const feed = feedOf(user, posts);
        expect(feed.posts).toStrictEqual(posts);
        expect(feed.owner).toStrictEqual(user);
    });

    test("post contains keywords", () => {
        const user = userOf(username, email);
        const post = postOf(user, content);
        expect(post.contains("test")).toBe(true);
    });

    test("feed can be filtered by keywords", () => {
        const user = userOf(username, email);
        const post1 = postOf(user, content);
        const post2 = postOf(user, "without keyword");
        const feed = feedOf(user, [post1, post2]);
        const filtered = feedOf(user, [post1]);
        expect(feed.filterBy("test")).toStrictEqual(filtered);
    });
});
