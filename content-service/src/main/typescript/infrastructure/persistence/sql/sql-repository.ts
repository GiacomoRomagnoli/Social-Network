import {Connection, ConnectionOptions, createConnection, ResultSetHeader, RowDataPacket} from 'mysql2/promise';
import {
    Feed,
    feedOf,
    Friendship,
    FriendshipID,
    friendshipOf,
    Post,
    postFrom,
    User,
    userOf
} from "../../../domain/domain";
import {
    INSERT_POST,
    INSERT_USER,
    FIND_POST_BY_ID,
    FIND_ALL_POST,
    GET_FEED,
    INSERT_FRIENDSHIP,
    FIND_USER_BY_ID,
    FIND_FRIENDSHIP_BY_ID,
    DELETE_USER_BY_ID,
    FIND_ALL_USERS,
    UPDATE_USER,
    DELETE_FRIENDSHIP_BY_ID,
    FIND_ALL_FRIENDSHIP, DELETE_POST_BY_ID, UPDATE_POST
} from "./sql-operations";
import {Connectable, FriendshipRepository, PostRepository, UserRepository} from "../repository";
import {social} from "../../../commons-lib";
import ID = social.common.ddd.ID;


abstract class SqlConnection implements Connectable {
    protected connection?: Connection;

    async connect(config: ConnectionOptions) {
        this.connection = await createConnection(config);
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
            const result = await this.connection.execute<PostDTO[]>(FIND_POST_BY_ID, [id.id]);
            const posts = this.mapToPost(result[0])
            return posts.length === 1 ? posts[0] : undefined
        }
        return undefined;
    }

    async deleteById(id: ID<string>): Promise<Post | undefined> {
        if(this.connection) {
            await this.connection.beginTransaction();
            const post = await this.findByID(id);
            if(post){
                await this.connection.execute(DELETE_POST_BY_ID, [id.id]);
            }
            await this.connection.commit();
            return post;
        }
        return undefined
    }

    async findAll(): Promise<Post[]> {
        if(this.connection) {
            const result = await this.connection.execute<PostDTO[]>(FIND_ALL_POST);
            return this.mapToPost(result[0]);
        }
        return [];
    }

    async update(post: Post): Promise<void> {
        await this.connection?.execute(UPDATE_POST, [post.author.email, post.content, post.id.id]);
    }

    async getFeed(user: User): Promise<Feed> {
        if(this.connection) {
            const result = await this.connection.execute<PostDTO[]>(GET_FEED, [user.email, user.email]);
            return feedOf(user, this.mapToPost(result[0]));
        }
        return feedOf(user, []);
    }
}

export class SqlUserRepository extends SqlConnection implements UserRepository {

    private mapToUser(array: UserDTO[]): User[] {
        return array.map(dto => userOf(dto.userName, dto.email))
    }

    async save(user: User) {
        await this.connection?.execute<ResultSetHeader>(INSERT_USER, [user.userName, user.email]);
    }

    async findByID(id: ID<string>): Promise<User | undefined> {
        if(this.connection){
            const result = await this.connection.execute<UserDTO[]>(FIND_USER_BY_ID, [id.id]);
            const users = this.mapToUser(result[0]);
            return users.length === 1 ? users[0] : undefined;
        }
        return undefined
    }

    async deleteById(id: ID<string>): Promise<User | undefined> {
        if(this.connection) {
            await this.connection.beginTransaction();
            const user = await this.findByID(id);
            if(user){
                await this.connection.execute(DELETE_USER_BY_ID, [id.id]);
            }
            await this.connection.commit();
            return user;
        }
        return undefined
    }

    async findAll(): Promise<User[]> {
        if(this.connection) {
            const result = await this.connection.execute<UserDTO[]>(FIND_ALL_USERS);
            return this.mapToUser(result[0]);
        }
        return [];
    }

    async update(user: User): Promise<void> {
        await this.connection?.execute(UPDATE_USER, [user.userName, user.email]);
    }
}

export class SqlFriendshipRepository extends SqlConnection implements FriendshipRepository {
    private mapToFriendship(array: FriendshipDTO[]): Friendship[] {
        return array
            .map(dto => friendshipOf(userOf(dto.userName1, dto.user1), userOf(dto.userName2, dto.user2)))
    }

    async deleteById(id: FriendshipID): Promise<Friendship | undefined> {
        if(this.connection) {
            await this.connection.beginTransaction();
            const friendship = await this.findByID(id);
            if(friendship) {
                await this.connection.execute(DELETE_FRIENDSHIP_BY_ID, [id.user1, id.user2]);
            }
            await this.connection.commit();
            return friendship;
        }
        return undefined
    }

    async findAll(): Promise<Friendship[]> {
        if(this.connection) {
            const result = await this.connection.execute<FriendshipDTO[]>(FIND_ALL_FRIENDSHIP);
            return this.mapToFriendship(result[0]);
        }
        return [];
    }

    async findByID(id: FriendshipID): Promise<Friendship | undefined> {
        if(this.connection) {
            const result =
                await this.connection.execute<FriendshipDTO[]>(FIND_FRIENDSHIP_BY_ID, [id.user1, id.user2]);
            const friendships = this.mapToFriendship(result[0]);
            return friendships.length === 1 ? friendships[0] : undefined;
        }
        return undefined;
    }

    async save(friendship: Friendship): Promise<void> {
        await this.connection?.execute<ResultSetHeader>(INSERT_FRIENDSHIP, [friendship.user1.email, friendship.user2.email]);
    }

    // this operation is actually a save in this domain
    async update(friendship: Friendship): Promise<void> {
        await this.save(friendship);
    }

}

interface UserDTO extends RowDataPacket {
    readonly email: string;
    readonly userName: string;
}

interface PostDTO extends RowDataPacket {
    readonly id: string;
    readonly userName: string;
    readonly author: string;
    readonly content: string;
}

interface FriendshipDTO extends RowDataPacket {
    readonly user1: string;
    readonly userName1: string;
    readonly user2: string;
    readonly userName2: string;
}