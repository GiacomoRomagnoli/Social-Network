package social.notification.infrastructure.serialization.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import social.common.events.FriendshipRequestRejected

object FriendRequestRejectedSerializer : JsonSerializer<FriendshipRequestRejected>() {
    override fun serialize(event: FriendshipRequestRejected, gen: JsonGenerator, serializers: SerializerProvider?) {
        gen.writeStartObject()
        gen.writeStringField("type", FriendshipRequestRejected.TOPIC)
        gen.writeStringField("sender", event.sender)
        gen.writeStringField("receiver", event.receiver)
        gen.writeEndObject()
    }
}
