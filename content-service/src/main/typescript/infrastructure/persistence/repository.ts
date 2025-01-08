import {Post, User} from "../../domain/domain";
import {social} from "../../commons-lib";
import Entity = social.common.ddd.Entity;
import ID = social.common.ddd.ID;

export interface Repository<I, T extends Entity<ID<I>>> {
    save(entity: T): Promise<void>
    findByID(id: ID<I>): Promise<T | undefined>
    deleteById(id: I): Promise<T | undefined>
    findAll(): Promise<T[]>
    update(entity: T): Promise<void>
}

export interface Connectable {
    connect(host: string, service: string, username: string, password: string): Promise<void>;
}

export interface PostRepository extends Repository<string, Post>, Connectable {}

export interface UserRepository extends Repository<string, User>, Connectable {}