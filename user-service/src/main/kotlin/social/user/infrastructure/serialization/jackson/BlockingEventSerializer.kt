package social.user.infrastructure.serialization.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import social.common.events.BlockingEvent
import social.common.events.UserBlocked
import social.common.events.UserUnblocked

object BlockingEventSerializer : JsonSerializer<BlockingEvent>() {
    override fun serialize(event: BlockingEvent, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField("user", event.user)
        gen.writeStringField(
            "op",
            when (event) {
                is UserBlocked -> "blocked"
                is UserUnblocked -> "unblocked"
                else -> throw JsonParseException("${event::class.simpleName} cannot be serialized")
            }
        )
        gen.writeEndObject()
    }
}
