import {ResultSetHeader, RowDataPacket} from 'mysql2/promise';
import {
    Feed,
    feedOf,
    Friendship,
    FriendshipID,
    friendshipOf,
    Post,
    postFrom,
    User, UserID,
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
    FIND_ALL_FRIENDSHIP, DELETE_POST_BY_ID, UPDATE_POST, FIND_ALL_POST_BY_AUTHOR
} from "./sql-operations";
import {FriendshipRepository, PostRepository, UserRepository} from "../../../application/repository";
import {social} from "../../../commons-lib";
import {SqlErrors} from "./sql-errors";
import ID = social.common.ddd.ID;
import {Probe} from "../../probes";

export class SqlPostRepository extends SqlErrors implements PostRepository, Probe {

    async isReady() {
        await this.connection!.execute<ResultSetHeader>("SELECT 1")
    }

    private mapToPost(array: PostDTO[]) {
        return array.map(dto => postFrom(dto.userName, dto.author, dto.content, dto.id));
    }

    async save(post: Post) {
        try {
            await this.connection!.execute<ResultSetHeader>(INSERT_POST, [post.author.id.value, post.content, post.id.value]);
        } catch (error) {
            this.throwErrorFor(error);
        }
    }

    async findByID(id: ID<string>): Promise<Post | undefined>{
        try {
            const result = await this.connection!.execute<PostDTO[]>(FIND_POST_BY_ID, [id.value]);
            const posts = this.mapToPost(result[0])
            return posts.length === 1 ? posts[0] : undefined
        } catch (error) {
            this.throwErrorFor(error);
        }
    }

    async deleteById(id: ID<string>): Promise<Post | undefined> {
        try {
            await this.connection!.beginTransaction();
            const post = await this.findByID(id);
            if(post){
                await this.connection!.execute(DELETE_POST_BY_ID, [id.value]);
            }
            await this.connection!.commit();
            return post;
        } catch (error) {
            this.throwErrorFor(error);
        }
    }

    async findAll(): Promise<Post[]> {
        try {
            const result = await this.connection!.execute<PostDTO[]>(FIND_ALL_POST);
            return this.mapToPost(result[0]);
        } catch (error) {
            this.throwErrorFor(error);
        }
        return [];
    }

    async update(post: Post): Promise<void> {
        try {
            await this.connection!.execute(UPDATE_POST, [post.author.id.value, post.content, post.id.value]);
        } catch (error) {
            this.throwErrorFor(error);
        }
    }

    async getFeed(user: User): Promise<Feed> {
        try {
            const result = await this.connection!.execute<PostDTO[]>(GET_FEED, [user.id.value, user.id.value]);
            return feedOf(user, this.mapToPost(result[0]));
        } catch (error) {
            this.throwErrorFor(error);
        }
        return feedOf(user, []);
    }

    async findAllPostsByUserID(id: UserID): Promise<Post[]> {
        try {
            const result = await this.connection!.execute<PostDTO[]>(FIND_ALL_POST_BY_AUTHOR, [id.value]);
            return this.mapToPost(result[0]);
        } catch (error) {
            this.throwErrorFor(error);
        }
        return [];
    }
}

export class SqlUserRepository extends SqlErrors implements UserRepository, Probe {

    async isReady() {
        await this.connection!.execute<ResultSetHeader>("SELECT 1")
    }

    private mapToUser(array: UserDTO[]): User[] {
        return array.map(dto => userOf(dto.userName, dto.email))
    }

    async save(user: User) {
        try {
            await this.connection!.execute<ResultSetHeader>(INSERT_USER, [user.userName, user.id.value]);
        } catch (error) {
            this.throwErrorFor(error);
        }
    }

    async findByID(id: ID<string>): Promise<User | undefined> {
        try {
            const result = await this.connection!.execute<UserDTO[]>(FIND_USER_BY_ID, [id.value]);
            const users = this.mapToUser(result[0]);
            return users.length === 1 ? users[0] : undefined;
        } catch (error) {
            this.throwErrorFor(error);
        }
    }

    async deleteById(id: ID<string>): Promise<User | undefined> {
        try {
            await this.connection!.beginTransaction();
            const user = await this.findByID(id);
            if(user){
                await this.connection!.execute(DELETE_USER_BY_ID, [id.value]);
            }
            await this.connection!.commit();
            return user;
        } catch (error) {
            this.throwErrorFor(error);
        }
    }

    async findAll(): Promise<User[]> {
        try {
            const result = await this.connection!.execute<UserDTO[]>(FIND_ALL_USERS);
            return this.mapToUser(result[0]);
        } catch (error) {
            this.throwErrorFor(error);
        }
        return [];
    }

    async update(user: User): Promise<void> {
        try {
            await this.connection!.execute(UPDATE_USER, [user.userName, user.id.value]);
        } catch (error) {
            this.throwErrorFor(error);
        }
    }
}

export class SqlFriendshipRepository extends SqlErrors implements FriendshipRepository, Probe {
    async isReady() {
        await this.connection!.execute<ResultSetHeader>("SELECT 1")
    }

    private mapToFriendship(array: FriendshipDTO[]): Friendship[] {
        return array
            .map(dto => friendshipOf(userOf(dto.userName1, dto.user1), userOf(dto.userName2, dto.user2)))
    }

    async deleteById(id: FriendshipID): Promise<Friendship | undefined> {
        try {
            await this.connection!.beginTransaction();
            const friendship = await this.findByID(id);
            if(friendship) {
                await this.connection!.execute(DELETE_FRIENDSHIP_BY_ID, [id.user1, id.user2]);
            }
            await this.connection!.commit();
            return friendship;
        } catch (error) {
            this.throwErrorFor(error);
        }
    }

    async findAll(): Promise<Friendship[]> {
        try {
            const result = await this.connection!.execute<FriendshipDTO[]>(FIND_ALL_FRIENDSHIP);
            return this.mapToFriendship(result[0]);
        } catch (error) {
            this.throwErrorFor(error);
        }
        return [];
    }

    async findByID(id: FriendshipID): Promise<Friendship | undefined> {
        try {
            const result =
                await this.connection!.execute<FriendshipDTO[]>(FIND_FRIENDSHIP_BY_ID, [id.user1, id.user2]);
            const friendships = this.mapToFriendship(result[0]);
            return friendships.length === 1 ? friendships[0] : undefined;
        } catch (error) {
            this.throwErrorFor(error);
        }
    }

    async save(friendship: Friendship): Promise<void> {
        try {
            await this.connection?.execute<ResultSetHeader>(INSERT_FRIENDSHIP, [friendship.user1.id.value, friendship.user2.id.value]);
        } catch (error: any) {
            this.throwErrorFor(error);
        }
    }

    // this operation is actually a save in this domain
    async update(friendship: Friendship): Promise<void> {
        try {
            await this.save(friendship);
        } catch (error) {
            this.throwErrorFor(error);
        }
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