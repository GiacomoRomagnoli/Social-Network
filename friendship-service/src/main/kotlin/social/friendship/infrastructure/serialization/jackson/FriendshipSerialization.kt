package social.friendship.social.friendship.infrastructure.serialization.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import social.friendship.domain.Friendship
import social.friendship.domain.User

object FriendshipSerializer : JsonSerializer<Friendship>() {
    override fun serialize(entity: Friendship, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeFieldName("user1")
        UserSerializer.serialize(entity.user1, gen, serializers)
        gen.writeFieldName("user2")
        UserSerializer.serialize(entity.user2, gen, serializers)
        gen.writeEndObject()
    }
}

object FriendshipDeserializer : JsonDeserializer<Friendship>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Friendship {
        val node = p.codec.readTree<JsonNode>(p)
        val user1 = User.of(node["user1"]?.asText() ?: throw JsonParseException("missing user1 field"))
        val user2 = User.of(node["user2"]?.asText() ?: throw JsonParseException("missing user2 field"))
        return Friendship.of(user1, user2)
    }
}
