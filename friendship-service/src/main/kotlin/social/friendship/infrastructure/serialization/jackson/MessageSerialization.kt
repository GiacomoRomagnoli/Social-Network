package social.friendship.social.friendship.infrastructure.serialization.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import social.friendship.domain.Message
import social.friendship.domain.User
import java.time.Instant
import java.util.UUID

object MessageSerializer : JsonSerializer<Message>() {
    override fun serialize(entity: Message, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField("id", entity.id.toString())
        gen.writeFieldName("sender")
        UserSerializer.serialize(entity.sender, gen, serializers)
        gen.writeFieldName("receiver")
        UserSerializer.serialize(entity.receiver, gen, serializers)
        gen.writeStringField("content", entity.content)
        gen.writeObjectField("timestamp", entity.timestamp)
        gen.writeEndObject()
    }
}

object MessageDeserializer : JsonDeserializer<Message>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Message {
        val node = p.codec.readTree<JsonNode>(p)
        val timestampNode = node["timestamp"]
        val idNode = node["id"]
        val sender = User.of(node["sender"]?.asText() ?: throw JsonParseException("missing sender field"))
        val receiver = User.of(node["receiver"]?.asText() ?: throw JsonParseException("missing receiver field"))
        val content = node["content"]?.asText() ?: throw JsonParseException("missing content field")
        return when {
            timestampNode == null && idNode == null -> Message.of(sender, receiver, content)
            timestampNode == null -> Message.of(UUID.fromString(idNode.asText()), sender, receiver, content)
            else -> Message.of(
                UUID.fromString(idNode.asText()),
                sender,
                receiver,
                content,
                Instant.parse(timestampNode.asText())
            )
        }
    }
}
