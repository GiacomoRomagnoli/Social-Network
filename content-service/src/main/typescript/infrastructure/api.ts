import express from "express";
import http from "http";

class Server {
    private readonly port: number;
    private readonly middlewares: express.RequestHandler[];
    private readonly router: express.Router;
    public server?: http.Server;

    constructor(port: number, middlewares: express.RequestHandler[], router: express.Router) {
        this.port = port;
        this.middlewares = middlewares;
        this.router = router;
    }

    async start(onStarted: () => void = () => {}) {
        return new Promise<void>((resolve) => {
            const app = express();
            app.use(...this.middlewares);
            app.use("/", this.router);
            this.server = http.createServer(app)
            this.server.listen(this.port, () => {
               onStarted();
               resolve();
            });
        });
    }

    async stop() {
        return new Promise<void>((resolve) => {
            if(this.server) {
                this.server.close(() => {
                   resolve();
                });
            } else {
                resolve();
            }
        });
    }
}
