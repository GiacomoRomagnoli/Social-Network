import {afterEach, beforeEach, describe, expect, test} from "@jest/globals";
import {ContentService, ContentServiceImpl} from "../../../main/typescript/application/service";
import {
    SqlFriendshipRepository,
    SqlPostRepository, SqlUserRepository
} from "../../../main/typescript/infrastructure/persistence/sql/sql-repository";
import {Kafka} from "kafkajs";
import {runConsumer} from "../../../main/typescript/infrastructure/kafka";
import {social} from "../../../main/typescript/commons-lib";
import UserCreated = social.common.events.UserCreated;
import {UserRepository} from "../../../main/typescript/application/repository";
import {UserID} from "../../../main/typescript/domain/domain";

describe("api module", () => {
    const shell = require("shelljs");
    let service: ContentService;
    let userRepository: UserRepository;
    const kafka = new Kafka({
        clientId: "user-service",
        brokers: ["127.0.0.1:9092"],
    });
    const producer = kafka.producer()
    shell.cd("src/test/typescript/infrastructure");

    beforeEach(async () => {
        const result = shell.exec("docker compose up --wait");
        console.log(result);
        userRepository = new SqlUserRepository();
        service = new ContentServiceImpl(
            new SqlFriendshipRepository(),
            new SqlPostRepository(),
            userRepository
        );
        await service.init(3307);
        await producer.connect();
    }, 180 * 1000);

    afterEach(async () => {
        await producer.disconnect();
        const result = shell.exec("docker compose down -v");
        console.log(result);
    });

    test("find docker file", async () => {
        const message = JSON.stringify(new UserCreated("John Doe", "johndoe@example.com"));
        await producer.send({
            topic: UserCreated.Companion.TOPIC,
            messages: [
                { value: message },
            ],
        });
        await runConsumer(service);
        const user = await userRepository.findByID(new UserID("johndoe@example.com"));
        expect(user).toBeDefined();
    });
});