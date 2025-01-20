import {afterEach, beforeEach, describe, expect, test} from "@jest/globals";
import {Kafka, Producer} from "kafkajs";
import {social} from "../../../main/typescript/commons-lib";
import UserCreated = social.common.events.UserCreated;
import FriendshipRequestAccepted = social.common.events.FriendshipRequestAccepted;
import {KafkaConsumer} from "../../../main/typescript/infrastructure/kafka";
import {ContentService, ContentServiceImpl} from "../../../main/typescript/application/service";
import {
    SqlFriendshipRepository,
    SqlPostRepository, SqlUserRepository
} from "../../../main/typescript/infrastructure/persistence/sql/sql-repository";
import {UserRepository} from "../../../main/typescript/application/repository";
import {UserID} from "../../../main/typescript/domain/domain";

describe("kafka module", () => {
    const shell = require("shelljs");
    let service: ContentService;
    let userRepository: UserRepository;
    let producer: Producer;
    let kafka: Kafka;
    let consumer: KafkaConsumer;
    shell.cd("src/test/typescript/infrastructure");

    beforeEach(async () => {
        const result = shell.exec("docker compose up --wait");
        console.log(result);
        kafka = new Kafka({
            clientId: "test",
            brokers: ["localhost:9092"]
        });
        userRepository = new SqlUserRepository();
        service = new ContentServiceImpl(
            new SqlFriendshipRepository(),
            new SqlPostRepository(),
            userRepository
        )
        await service.init(3307);
        producer = kafka.producer();
        consumer = new KafkaConsumer(kafka, {groupId: "content-service-group"}, service);
    }, 180 * 1000);

    afterEach(async () => {
        await producer.disconnect();
        await consumer.stop();
        const result = shell.exec("docker compose down -v");
        console.log(result);
    });

    test("content-service consumer", async () => {
        const admin = kafka.admin();
        await admin.connect()
        await admin.createTopics({
            topics: [
                {topic: UserCreated.Companion.TOPIC},
                {topic: FriendshipRequestAccepted.Companion.TOPIC}
            ]
        });
        await admin.disconnect();

        await producer.connect();
        await producer.send({
            topic: UserCreated.Companion.TOPIC,
            messages: [
                {
                    value: JSON.stringify(new UserCreated("John Doe", "johndoe@example.com")),
                }
            ]
        });

        await consumer.consume();

        await new Promise<void>((resolve) => {
            consumer.emitter.once("done", () => resolve())
        });

        const user = userRepository.findByID(new UserID("johndoe@example.com"));
        expect(user).toBeDefined();
    }, 120 * 1000);
});