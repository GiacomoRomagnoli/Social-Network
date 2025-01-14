import { Router, Request, Response } from "express";
import {ContentService} from "../application/service";
import {postFrom} from "../domain/domain";
import {NoReferencedRowError} from "./persistence/sql/sql-errors";
import {social} from "../commons-lib";
import StatusCode = social.common.endpoint.StatusCode;

export function getRouter(service: ContentService): Router {
    const router = Router();

    router.post('/contents/posts', async (req: Request, res: Response) => {
        const body = req.body;
        if(isPost(body)) {
            try {
                await service.addPost(postFrom(body.user.name, body.user.email, body.content));
                res.status(StatusCode.OK);
            } catch (error) {
                if (error instanceof NoReferencedRowError) {
                    res.status(StatusCode.FORBIDDEN).json(error.message);
                } else {
                    res.status(StatusCode.INTERNAL_SERVER_ERROR);
                }
            }
        } else {
            res.status(StatusCode.BAD_REQUEST);
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