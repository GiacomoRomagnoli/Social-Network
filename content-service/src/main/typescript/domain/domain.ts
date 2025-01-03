import {social} from "../commons-lib";
import AggregateRoot = social.common.ddd.AggregateRoot;
import ID = social.common.ddd.ID;
import Entity = social.common.ddd.Entity;

export interface User {
    readonly userName: string;
    readonly email: string;
    readonly id: ID<string>;
}

class UserImpl extends Entity<ID<string>> implements User {
    readonly userName: string;
    readonly email: string;

    constructor(userName: string, email: string) {
        super(new ID(email))
        this.userName = userName;
        this.email = email;
    }
}

export interface Post {
    readonly author: User;
    readonly content: string;
    readonly id: ID<number>;
    contains(keyword: string): boolean;
}

class PostImpl extends AggregateRoot<ID<number>> implements Post {
    readonly author: User;
    readonly content: string;

    constructor(author: User, content: string, id: number) {
        super(new ID(id));
        this.author = author;
        this.content = content;
    }

    contains(keyword: string): boolean {
        return this.content.includes(keyword);
    }
}

export interface Feed {
    readonly posts: Post[];
    readonly id: ID<string>;
    filterBy(keyword: string): Feed;
    copy(owner: string, posts: Post[]): Feed;
}

class FeedImpl extends AggregateRoot<ID<string>> implements Feed {
    readonly posts: Post[];

    constructor(owner: string, posts: Post[]) {
        super(new ID(owner));
        this.posts = posts;
    }

    filterBy(keyword: string): Feed {
        return this.copy(undefined, this.posts.filter(post => post.contains(keyword)));
    }

    copy(owner= this.id.id, posts = this.posts): Feed {
        return new FeedImpl(owner, posts);
    }
}

// Factory methods

export function postOf(authorName: string, authorEmail: string, content: string, id: number): Post {
    return new PostImpl(new UserImpl(authorName, authorEmail), content, id);
}

export function feedFrom(owner: string, ...posts: Post[]): Feed {
    return new FeedImpl(owner, posts);
}

export function feedOf(owner: string, posts: Post[]): Feed {
    return new FeedImpl(owner, posts);
}
