import {social} from "./commons-lib";
import StatusCode = social.common.endpoint.StatusCode;
import {Server, DefaultMiddlewares} from "./infrastructure/api"
import {router} from "./infrastructure/router"

console.log('Hello, world!');

console.log(StatusCode.OK)

new Server(8080, DefaultMiddlewares, router).start().then(() => console.log("server up!"))
