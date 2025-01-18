import {Kafka} from "kafkajs";
import {social} from "../commons-lib";
import UserCreated = social.common.events.UserCreated;
import FriendshipRequestAccepted = social.common.events.FriendshipRequestAccepted;
import {ContentService} from "../application/service";
import {friendshipOf, userOf} from "../domain/domain";

const kafka = new Kafka({
    clientId: "content-service",
    brokers: ["127.0.0.1:9092"],
    connectionTimeout: 3000,
    retry: {
        retries: 5,
    },
});

const consumer = kafka.consumer({
    groupId: 'content-service-group',
});

export async function runConsumer(service: ContentService){
    await consumer.connect();
    await consumer.subscribe({
        topics: [UserCreated.Companion.TOPIC, FriendshipRequestAccepted.Companion.TOPIC],
        fromBeginning: true,
    });
    await consumer.run({
        autoCommit: false,
        eachMessage: async ({ topic, partition, message }) => {
            if(message.value){
                const json = JSON.parse(message.value.toString());
                console.log(json);
                switch (topic) {
                    case UserCreated.Companion.TOPIC:
                        await service.addUser(userOf(json.username, json.email));
                        break;
                    case FriendshipRequestAccepted.Companion.TOPIC:
                        // for sql repository username is ignored but consider another strategy for mongo
                        const user1 = userOf("ignored", json.sender);
                        const user2 = userOf("ignored", json.receiver);
                        await service.addFriendship(friendshipOf(user1, user2));
                        break;
                }
            }
            await consumer.commitOffsets([
                { topic, partition, offset: (Number(message.offset) + 1).toString() },
            ]);
        },
    });
}