package social.notification.infrastructure.serialization.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import social.common.events.MessageSent

object MessageSentSerializer : JsonSerializer<MessageSent>() {
    override fun serialize(event: MessageSent, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField("type", MessageSent.TOPIC)
        gen.writeStringField("id", event.id)
        gen.writeStringField("sender", event.sender)
        gen.writeStringField("receiver", event.receiver)
        gen.writeStringField("message", event.message)
        gen.writeStringField("timestamp", event.timestamp)
        gen.writeEndObject()
    }
}
