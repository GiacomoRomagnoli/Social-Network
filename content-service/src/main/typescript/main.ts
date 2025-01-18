import {Server, DefaultMiddlewares} from "./infrastructure/api"
import {getRouter} from "./infrastructure/router"
import {ContentServiceImpl} from "./application/service";
import {
    SqlFriendshipRepository,
    SqlPostRepository,
    SqlUserRepository
} from "./infrastructure/persistence/sql/sql-repository";
import {FriendshipRepository, PostRepository, UserRepository} from "./application/repository";

const userRepository: UserRepository = new SqlUserRepository();
const postRepository: PostRepository = new SqlPostRepository();
const friendshipRepository: FriendshipRepository = new SqlFriendshipRepository();
const service = new ContentServiceImpl(friendshipRepository, postRepository,userRepository);
service.init(3306).then(() => {
    const server = new Server(8080, DefaultMiddlewares, getRouter(service));
    server.start().then(() => console.log("server up!"));
});

