import {ConsumerConfig, ConsumerSubscribeTopics, Kafka} from "kafkajs";
import {EventEmitter} from "events";
import {social} from "../commons-lib";
import UserCreated = social.common.events.UserCreated;
import FriendshipRequestAccepted = social.common.events.FriendshipRequestAccepted;
import {ContentService} from "../application/service";
import {friendshipOf, userOf} from "../domain/domain";

type Handler = (topic: string, json: any, service: ContentService) => Promise<void>;

const defaultSubscription: ConsumerSubscribeTopics =
    {
        topics: [UserCreated.Companion.TOPIC, FriendshipRequestAccepted.Companion.TOPIC],
        fromBeginning: true,
    }

const defaultHandler: Handler =
    async (topic: string, json: any, service: ContentService) => {
        switch (topic) {
            case UserCreated.Companion.TOPIC:
                await service.addUser(userOf(json.username, json.email));
                break;
            case FriendshipRequestAccepted.Companion.TOPIC:
                const user1 = userOf("", json.sender);
                const user2 = userOf("", json.receiver);
                await service.addFriendship(friendshipOf(user1, user2));
                break;
            default:
                console.log(`unexpected topic: ${topic}`);
                break;
        }
    }

export class KafkaConsumer {
    private readonly consumer;
    readonly emitter;
    private readonly service;

    constructor(kafka: Kafka, consumerConfig: ConsumerConfig, service: ContentService) {
        this.service = service;
        this.emitter = new EventEmitter();
        this.consumer = kafka.consumer(consumerConfig);
    }

    async consume(subscription = defaultSubscription, handler = defaultHandler) {
        await this.consumer.connect();
        await this.consumer.subscribe(subscription);
        await this.consumer.run({
            eachMessage:  async ({topic, message}) => {
                if(message.value){
                    const json = JSON.parse(message.value.toString());
                    await handler(topic, json, this.service);
                }
                this.emitter.emit("done");
            },
        });
    }

    async stop() {
        this.consumer.disconnect();
    }

}