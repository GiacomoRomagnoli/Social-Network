import {afterEach, beforeEach, describe, expect, test} from "@jest/globals";
import{EventEmitter} from "events"
import {Kafka} from "kafkajs";
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
    shell.cd("src/test/typescript/infrastructure");

    beforeEach(async () => {
        const result = shell.exec("docker compose up --wait");
        console.log(result);
        userRepository = new SqlUserRepository();
        service = new ContentServiceImpl(
            new SqlFriendshipRepository(),
            new SqlPostRepository(),
            userRepository
        )
        await service.init(3307);
    }, 180 * 1000);

    afterEach(async () => {
        const result = shell.exec("docker compose down -v");
        console.log(result);
    });

    test("receive a user created topic", async () => {
        const emitter = new EventEmitter()

        const kafka = new Kafka({
            clientId: "test",
            brokers: ["localhost:9092"]
        })

        const kafka2 = new Kafka({
            clientId: "redundant",
            brokers: ["localhost:9092"]
        })

        const consumer = kafka.consumer({groupId: "content-service-group"})
        const producer = kafka2.producer();
        const admin = kafka.admin();

        await admin.connect()
        await admin.createTopics({topics: [{topic: "topic"}, {topic: "topa"}]})
        await admin.disconnect()

        await producer.connect()
        await producer.send({topic: "topic", messages: [{value: "message"}]})

        await consumer.connect()
        await consumer.subscribe({topics: ["topic", "topa"], fromBeginning: true})
        await consumer.run({
            eachMessage: async ({topic, message}) => {
                console.log(topic);
                console.log(message?.value);
                emitter.emit("done");
            }
        })
        return new Promise<void>((resolve) => {
            emitter.once("done", () => resolve())
        })
    }, 90 * 1000);

    test("content-service consumer", async () => {
        const kafka = new Kafka({
            clientId: "test",
            brokers: ["localhost:9092"]
        });

        const admin = kafka.admin();
        await admin.connect()
        await admin.createTopics({
            topics: [
                {topic: UserCreated.Companion.TOPIC},
                {topic: FriendshipRequestAccepted.Companion.TOPIC}
            ]
        });
        await admin.disconnect();

        const producer = kafka.producer();
        await producer.connect();
        await producer.send({
            topic: UserCreated.Companion.TOPIC,
            messages: [
                {
                    value: JSON.stringify(new UserCreated("John Doe", "johndoe@example.com")),
                }
            ]
        });

        const consumer = new KafkaConsumer(kafka, {groupId: "content-service-group"}, service);
        await consumer.consume();

        await new Promise<void>((resolve) => {
            consumer.emitter.once("done", () => resolve())
        });

        const user = userRepository.findByID(new UserID("johndoe@example.com"));
        expect(user).toBeDefined();
    }, 120 * 1000);
});