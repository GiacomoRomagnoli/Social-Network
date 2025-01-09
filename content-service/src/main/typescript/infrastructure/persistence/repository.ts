import {Feed, Post, User} from "../../domain/domain";
import {social} from "../../commons-lib";
import Entity = social.common.ddd.Entity;
import ID = social.common.ddd.ID;

export interface Repository<T, I extends ID<T>, E extends Entity<I>> {
    save(entity: E): Promise<void>
    findByID(id: I): Promise<E | undefined>
    deleteById(id: I): Promise<E | undefined>
    findAll(): Promise<E[]>
    update(entity: E): Promise<void>
}

export interface Connectable {
    connect(host: string, service: string, username: string, password: string): Promise<void>;
}

export interface PostRepository extends Repository<string, ID<string>, Post>, Connectable {
    getFeed(user: User): Promise<Feed>
}

export interface UserRepository extends Repository<string, ID<string>, User>, Connectable {}