import {social} from "../commons-lib";
import AggregateRoot = social.common.ddd.AggregateRoot;
import ID = social.common.ddd.ID;

export interface Post {
    readonly author: string;
    readonly content: string;
    readonly id: ID<number>;
    contains(keyword: string): boolean;
}

class PostImpl extends AggregateRoot<ID<number>> implements Post {
    readonly author: string;
    readonly content: string;

    constructor(author: string, content: string, id: number) {
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

export function postOf(author: string, content: string, id: number): Post {
    return new PostImpl(author, content, id);
}

export function feedFrom(owner: string, ...posts: Post[]): Feed {
    return new FeedImpl(owner, posts);
}

export function feedOf(owner: string, posts: Post[]): Feed {
    return new FeedImpl(owner, posts);
}
