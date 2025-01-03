import {describe, expect, test} from '@jest/globals';
import {feedOf, postOf, feedFrom} from "../../main/typescript/domain/domain";
import {social} from "../../main/typescript/commons-lib";
import ID = social.common.ddd.ID;

describe("domain module", () => {
    let post = postOf("example", "example@mail.com", "my first post!", 0);
    let feed = feedOf("example@mail.com", [post, postOf("friend", "friend@mail.com", "another post", 1)])

    test("postOf creates a Post", () => {
        expect(post.author.userName).toBe("example")
        expect(post.author.email).toBe("example@mail.com")
        expect(post.content).toBe("my first post!")
        expect(post.id).toStrictEqual(new ID(0))
    });

    test("post contains keywords", () => {
        expect(post.contains("first")).toBe(true)
    });

    test("feedOf creates a Feed", () => {
        expect(feed.id).toStrictEqual(new ID("example@mail.com"))
        expect(feed.posts.length).toBe(2)
    });

    test("feedFrom creates a Feed", () => {
        let feed2 = feedFrom("friend@mail.com", post, postOf("example", "example@mail.com", "another one", 2))
        expect(feed2.id).toStrictEqual(new ID("friend@mail.com"))
        expect(feed2.posts.length).toBe(2)
    });

    test("Feed can be filter by keyword", () => {
       expect(feed.filterBy("first").posts.length).toBe(1)
    });
});
