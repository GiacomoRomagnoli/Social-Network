import {social} from "../commons-lib";
import {v4 as uuidv4} from "uuid"
import AggregateRoot = social.common.ddd.AggregateRoot;
import ID = social.common.ddd.ID;
import Entity = social.common.ddd.Entity;

export class UserID extends ID<string> {}

export interface User extends Entity<UserID> {
    readonly userName: string;
}

class UserImpl extends Entity<UserID> implements User {
    readonly userName: string;

    constructor(userName: string, email: string) {
        super(new UserID(email));
        this.userName = userName;
    }
}

export class PostID extends ID<string> {}

export interface Post extends AggregateRoot<PostID> {
    readonly author: User;
    readonly content: string;
    contains(keyword: string): boolean;
}

class PostImpl extends AggregateRoot<PostID> implements Post {
    readonly author: User;
    readonly content: string;

    constructor(author: User, content: string, id: string) {
        super(new PostID(id));
        this.author = author;
        this.content = content;
    }

    contains(keyword: string): boolean {
        return this.content.includes(keyword);
    }
}

export class FeedID extends ID<string> {}

export interface Feed {
    readonly posts: Post[];
    readonly owner: User;
    filterBy(keyword: string): Feed;
    copy(owner: User, posts: Post[]): Feed;
}

class FeedImpl extends AggregateRoot<FeedID> implements Feed {
    readonly posts: Post[];
    readonly owner: User;

    constructor(owner: User, posts: Post[]) {
        super(new FeedID(owner.id.value));
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

export interface Pair<X, Y> {
    x: X;
    y: Y;
}

export class FriendshipID extends ID<Pair<string, string>> {
    readonly user1: string;
    readonly user2: string;

    constructor(user1: string, user2: string) {
        const pair: Pair<string, string> = {x: user1, y: user2}
        super(pair);
        this.user1 = user1;
        this.user2 = user2;
    }
}

export interface Friendship extends Entity<FriendshipID> {
    readonly user1: User;
    readonly user2: User;
    readonly id: FriendshipID;
}

class FriendshipImpl extends Entity<FriendshipID> implements Friendship {
    readonly user1: User;
    readonly user2: User;

    constructor(user1: User, user2: User) {
        super(new FriendshipID(user1.id.value, user2.id.value));
        this.user1 = user1;
        this.user2 = user2;
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

export function friendshipOf(user1: User, user2: User): Friendship {
    return new FriendshipImpl(user1, user2);
}
