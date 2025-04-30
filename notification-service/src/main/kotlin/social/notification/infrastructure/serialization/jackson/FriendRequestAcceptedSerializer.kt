package social.notification.infrastructure.serialization.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import social.common.events.FriendshipRequestAccepted

object FriendRequestAcceptedSerializer : JsonSerializer<FriendshipRequestAccepted>() {
    override fun serialize(event: FriendshipRequestAccepted, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField("type", FriendshipRequestAccepted.TOPIC)
        gen.writeStringField("sender", event.sender)
        gen.writeStringField("receiver", event.receiver)
        gen.writeEndObject()
    }
}
