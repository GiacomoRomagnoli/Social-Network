import {Feed, Friendship, FriendshipID, Pair, Post, User, UserID} from "../domain/domain";
import {social} from "../commons-lib";
import Entity = social.common.ddd.Entity;
import ID = social.common.ddd.ID;
import fs from "node:fs";
import {ConnectionOptions} from "mysql2/promise";

export interface Repository<T, I extends ID<T>, E extends Entity<I>> {
    save(entity: E): Promise<void>
    findByID(id: I): Promise<E | undefined>
    deleteById(id: I): Promise<E | undefined>
    findAll(): Promise<E[]>
    update(entity: E): Promise<void>
}

export interface Connectable {
    connect(config: ConnectionOptions): Promise<void>;
}

export interface PostRepository extends Repository<string, ID<string>, Post>, Connectable {
    getFeed(user: User): Promise<Feed>;
    findAllPostsByUserID(id: UserID): Promise<Post[]>;
}

export interface UserRepository extends Repository<string, ID<string>, User>, Connectable {}

export interface FriendshipRepository extends Repository<Pair<string, string>, FriendshipID, Friendship>, Connectable {}

export function getConfiguration(port: number) : ConnectionOptions {
    return {
        host: "127.0.0.1",
        port: port,
        database: "content",
        user: "user",
        password: fs.readFileSync("./db-password.txt", 'utf8'),
    }
}