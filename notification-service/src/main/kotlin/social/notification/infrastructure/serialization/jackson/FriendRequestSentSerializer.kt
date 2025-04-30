package social.notification.infrastructure.serialization.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import social.common.events.FriendshipRequestSent

object FriendRequestSentSerializer : JsonSerializer<FriendshipRequestSent>() {
    override fun serialize(event: FriendshipRequestSent, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField("type", FriendshipRequestSent.TOPIC)
        gen.writeStringField("sender", event.sender)
        gen.writeStringField("receiver", event.receiver)
        gen.writeEndObject()
    }
}
