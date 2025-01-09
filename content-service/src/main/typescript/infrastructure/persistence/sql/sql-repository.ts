import {Connection, createConnection, ResultSetHeader, RowDataPacket} from 'mysql2/promise';
import {Feed, feedOf, Post, postFrom, User} from "../../../domain/domain";
import {INSERT_POST, INSERT_USER, FIND_POST_BY_ID, FIND_ALL_POST} from "./sql-operations";
import {Connectable, PostRepository, UserRepository} from "../repository";
import {social} from "../../../commons-lib";
import ID = social.common.ddd.ID;


abstract class SqlConnection implements Connectable {
    protected connection?: Connection;

    async connect(host: string, database: string, username: string, password: string) {
        this.connection = await createConnection({
            host: host,
            user: username,
            password: password,
            database: database
        });
    }
}

export class SqlPostRepository extends SqlConnection implements PostRepository {

    private mapToPost(array: PostDTO[]) {
        return array.map(dto => postFrom(dto.userName, dto.author, dto.content, dto.id));
    }

    async save(post: Post) {
       await this.connection?.execute<ResultSetHeader>(INSERT_POST, [post.author.email, post.content, post.id.id]);
    }

    async findByID(id: ID<string>): Promise<Post | undefined>{
        if (this.connection) {
            const result = await this.connection?.execute<PostDTO[]>(FIND_POST_BY_ID, [id.id]);
            const posts = this.mapToPost(result[0])
            return posts.length === 1 ? posts[0] : undefined
        }
        return undefined;
    }

    async deleteById(id: ID<string>): Promise<Post | undefined> {
        return undefined;
    }

    async findAll(): Promise<Post[]> {
        if(this.connection) {
            const result = await this.connection?.execute<PostDTO[]>(FIND_ALL_POST);
            return this.mapToPost(result[0]);
        }
        return [];
    }

    async update(entity: Post): Promise<void> {
        return undefined;
    }

    getFeed(user: User): Promise<Feed> {
        return Promise.resolve(feedOf(user, []));
    }
}

export class SqlUserRepository extends SqlConnection implements UserRepository {
    async save(user: User) {
        await this.connection?.execute<ResultSetHeader>(INSERT_USER, [user.userName, user.email])
    }

    async findByID(id: ID<string>): Promise<User | undefined> {
        return undefined;
    }

    async deleteById(id: ID<string>): Promise<User | undefined> {
        return undefined;
    }

    async findAll(): Promise<User[]> {
        return [];
    }

    async update(entity: User): Promise<void> {
        return undefined;
    }
}

interface UserDTO extends RowDataPacket {
    readonly userName: string,
    readonly email: string,
}

interface PostDTO extends RowDataPacket {
    readonly id: string,
    readonly userName: string,
    readonly author: string,
    readonly content: string,
}