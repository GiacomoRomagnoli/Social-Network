import {social} from "../commons-lib";
import Service = social.common.ddd.Service;
import {
    FriendshipRepository,
    getConfiguration,
    PostRepository,
    UserRepository
} from "../infrastructure/persistence/repository";
import {Feed, Friendship, FriendshipID, Post, PostID, User, UserID} from "../domain/domain";

export interface ContentService extends Service {
    addPost(post: Post): Promise<void>;
    addUser(user: User): Promise<void>;
    addFriendship(friendship: Friendship): Promise<void>;
    getPost(id: PostID): Promise<Post | undefined>;
    getFeed(user: User, keyword: string | undefined): Promise<Feed>;
    deletePost(id: PostID): Promise<Post | undefined>;
    deleteUser(id: UserID): Promise<User | undefined>;
    deleteFriendship(id: FriendshipID): Promise<Friendship | undefined>;
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

    deletePost(id: PostID): Promise<Post | undefined> {
        return this.postRepository.deleteById(id);
    }

    deleteUser(id: UserID): Promise<User | undefined> {
        return this.userRepository.deleteById(id);
    }

    async getFeed(user: User, keyword: string | undefined): Promise<Feed> {
        const feed = await this.postRepository.getFeed(user);
        if(keyword) {
            return feed.filterBy(keyword);
        }
        return feed;
    }

    getPost(id: PostID): Promise<Post | undefined> {
        return this.postRepository.findByID(id);
    }

}