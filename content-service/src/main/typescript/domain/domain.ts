import {social} from "../commons-lib";
import {v4 as uuidv4} from "uuid"
import AggregateRoot = social.common.ddd.AggregateRoot;
import ID = social.common.ddd.ID;
import Entity = social.common.ddd.Entity;

export interface User extends Entity<ID<string>> {
    readonly userName: string;
    readonly email: string;
    readonly id: ID<string>;
}

class UserImpl extends Entity<ID<string>> implements User {
    readonly userName: string;
    readonly email: string;

    constructor(userName: string, email: string) {
        super(new ID(email));
        this.userName = userName;
        this.email = email;
    }
}

export interface Post extends AggregateRoot<ID<string>> {
    readonly author: User;
    readonly content: string;
    readonly id: ID<string>;
    contains(keyword: string): boolean;
}

class PostImpl extends AggregateRoot<ID<string>> implements Post {
    readonly author: User;
    readonly content: string;

    constructor(author: User, content: string, id: string) {
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
    readonly owner: User;
    filterBy(keyword: string): Feed;
    copy(owner: User, posts: Post[]): Feed;
}

class FeedImpl extends AggregateRoot<ID<string>> implements Feed {
    readonly posts: Post[];
    readonly owner: User;

    constructor(owner: User, posts: Post[]) {
        super(owner.id);
        this.owner = owner;
        this.posts = posts;
    }

    filterBy(keyword: string): Feed {
        return this.copy(undefined, this.posts.filter(post => post.contains(keyword)));
    }

    copy(owner: User = this.owner, posts: Post[] = this.posts): Feed {
        return new FeedImpl(owner, posts);
    }
}

// Factory methods

export function postFrom(authorName: string, authorEmail: string, content: string, uuid?: string): Post {
    return postOf(userOf(authorName, authorEmail), content, uuid);
}

export function postOf(user: User, content: string, uuid?: string): Post {
    return new PostImpl(user, content, uuid ? uuid : uuidv4());
}

export function feedFrom(ownerName: string, ownerEmail: string, posts: Post[]): Feed {
    return feedOf(userOf(ownerName, ownerEmail), posts);
}

export function feedOf(owner: User, posts: Post[]): Feed {
    return new FeedImpl(owner, posts);
}

export function userOf(name: string, email: string): User {
    return new UserImpl(name, email);
}
