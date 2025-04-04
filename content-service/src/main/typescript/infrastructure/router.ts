import { Router, Request, Response } from "express";
import {ContentService} from "../application/service";
import {Feed, Post, postFrom, PostID, User, UserID} from "../domain/domain";
import {NoReferencedRowError} from "./persistence/sql/sql-errors";
import {social} from "../commons-lib";
import StatusCode = social.common.endpoint.StatusCode;
import {UnableToDelete} from "../application/service-errors";

export function getRouter(service: ContentService): Router {
    const router = Router();

    router.get("/health", (req: Request, res: Response) => {
        console.log("received health check request")
        res.status(StatusCode.OK).json("OK");
    });

    router.get("/contents/posts/:userID", async (req: Request, res: Response) => {
        try {
            const posts = await service.getPostByAuthor(new UserID(req.params.userID));
            res.status(StatusCode.OK).json(posts.map(p => postToJson(p)));
        } catch (error) {
            res.status(StatusCode.INTERNAL_SERVER_ERROR).end();
        }
    });

    router.post('/contents/posts', async (req: Request, res: Response) => {
        const body = req.body;
        if(isPost(body)) {
            try {
                await service.addPost(postFrom(body.user.name, body.user.email, body.content));
                res.status(StatusCode.CREATED).end();
            } catch (error) {
                if (error instanceof NoReferencedRowError) {
                    res.status(StatusCode.FORBIDDEN).json(error.message);
                } else {
                    res.status(StatusCode.INTERNAL_SERVER_ERROR).end();
                }
            }
        } else {
            res.status(StatusCode.BAD_REQUEST).end();
        }
    });

    router.get('/contents/posts/feed/:userID', async (req: Request, res: Response) => {
        try {
            if(req.query.keyword) {
                if(typeof req.query.keyword === "string") {
                    const feed = await service.getFeed(new UserID(req.params.userID), req.query.keyword);
                    res.status(StatusCode.OK).json(feedToJson(feed));
                } else {
                    res.status(StatusCode.BAD_REQUEST).json('keyword parameter is not a string');
                }
            } else {
                const feed = await service.getFeed(new UserID(req.params.userID));
                res.status(StatusCode.OK).json(feedToJson(feed));
            }
        } catch (error) {
            res.status(StatusCode.INTERNAL_SERVER_ERROR).end();
        }
    });

    router.delete('/contents/posts/:user/:post', async (req: Request, res: Response)=> {
        try {
            const post = await service.deletePost(new PostID(req.params.post), new UserID(req.params.user));
            res.status(StatusCode.OK).json(post ? postToJson(post) : post);
        } catch (error) {
            if(error instanceof UnableToDelete){
                res.status(StatusCode.FORBIDDEN).json(error.message);
            } else {
                res.status(StatusCode.INTERNAL_SERVER_ERROR).end();
            }
        }
    });

    return router;
}

function isUser(obj: any): boolean {
    return "name" in obj && "email" in obj;
}

function isPost(obj: any): boolean {
    return "user" in obj && isUser(obj.user) && "content" in obj;
}

function userToJson(user: User) {
    return {
        name: user.userName,
        email: user.id.value,
    }
}

function postToJson(post: Post) {
    return {
        id: post.id.value,
        author: userToJson(post.author),
        content: post.content,
    }
}

function feedToJson(feed: Feed) {
    return {
        owner: userToJson(feed.owner),
        posts: feed.posts.map(p => postToJson(p)),
    }
}
