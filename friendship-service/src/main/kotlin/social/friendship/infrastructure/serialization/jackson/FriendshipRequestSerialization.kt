package social.friendship.social.friendship.infrastructure.serialization.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import social.friendship.domain.FriendshipRequest
import social.friendship.domain.User

object FriendshipRequestSerializer : JsonSerializer<FriendshipRequest>() {
    override fun serialize(entity: FriendshipRequest, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeFieldName("from")
        UserSerializer.serialize(entity.from, gen, serializers)
        gen.writeFieldName("to")
        UserSerializer.serialize(entity.to, gen, serializers)
        gen.writeEndObject()
    }
}

object FriendshipRequestDeserializer : JsonDeserializer<FriendshipRequest>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): FriendshipRequest {
        val node = p.codec.readTree<JsonNode>(p)
        val from = User.of(node["from"]?.asText() ?: throw JsonParseException("missing from field"))
        val to = User.of(node["to"]?.asText() ?: throw JsonParseException("missing to field"))
        return FriendshipRequest.of(to, from)
    }
}
