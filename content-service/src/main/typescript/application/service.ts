import {social} from "../commons-lib";
import Service = social.common.ddd.Service;
import {
    FriendshipRepository,
    getConfiguration,
    PostRepository,
    UserRepository
} from "./repository";
import {Feed, Friendship, FriendshipID, Post, PostID, User, UserID} from "../domain/domain";
import {UnableToDelete} from "./service-errors";

export interface ContentService extends Service {
    addPost(post: Post): Promise<void>;
    addUser(user: User): Promise<void>;
    addFriendship(friendship: Friendship): Promise<void>;
    getPost(id: PostID): Promise<Post | undefined>;
    getFeed(userID: UserID, keyword?: string): Promise<Feed>;
    deletePost(postID: PostID, userID: UserID): Promise<Post | undefined>;
    deleteUser(id: UserID): Promise<User | undefined>;
    deleteFriendship(id: FriendshipID): Promise<Friendship | undefined>;
    getPostByAuthor(id: UserID): Promise<Post[]>;
    init(port: number): Promise<void>;
}

export class ContentServiceImpl implements ContentService {

    private friendshipRepository: FriendshipRepository;
    private postRepository: PostRepository;
    private userRepository: UserRepository;

    constructor(
        friendshipRepository: FriendshipRepository,
        postRepository: PostRepository,
        userRepository: UserRepository
    ) {
        this.friendshipRepository = friendshipRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    async init() {
        const config = getConfiguration();
        await this.friendshipRepository.connect(config);
        await this.postRepository.connect(config);
        await this.userRepository.connect(config);
    }

    addFriendship(friendship: Friendship) {
        return this.friendshipRepository.save(friendship);
    }

    addPost(post: Post) {
        return this.postRepository.save(post);
    }

    addUser(user: User) {
        return this.userRepository.save(user);
    }

    deleteFriendship(id: FriendshipID): Promise<Friendship | undefined> {
        return this.friendshipRepository.deleteById(id);
    }

    async deletePost(postID: PostID, userID: UserID): Promise<Post | undefined> {
        const post = await this.postRepository.findByID(postID);
        if(post) {
            if(userID.equals(post.author.id)){
                return await this.postRepository.deleteById(postID);
            } else {
                throw new UnableToDelete("only authors can delete posts");
            }
        }
        return undefined;
    }

    deleteUser(id: UserID): Promise<User | undefined> {
        return this.userRepository.deleteById(id);
    }

    async getFeed(userID: UserID, keyword?: string): Promise<Feed> {
        const user = await this.userRepository.findByID(userID);
        const feed = await this.postRepository.getFeed(user!);
        if(keyword) {
                return feed.filterBy(keyword);
        }
        return feed;
    }

    getPost(id: PostID): Promise<Post | undefined> {
        return this.postRepository.findByID(id);
    }

    getPostByAuthor(id: UserID): Promise<Post[]> {
        return this.postRepository.findAllPostsByUserID(id);
    }

}