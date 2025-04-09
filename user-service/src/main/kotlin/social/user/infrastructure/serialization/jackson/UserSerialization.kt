package social.user.infrastructure.serialization.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import social.user.domain.User

object UserSerializer : JsonSerializer<User>() {
    override fun serialize(entity: User, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField("email", entity.email)
        gen.writeStringField("username", entity.username)
        gen.writeBooleanField("isAdmin", entity.isAdmin)
        gen.writeBooleanField("isBlocked", entity.isBlocked)
        gen.writeEndObject()
    }
}

object UserDeserializer : JsonDeserializer<User>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): User {
        val node = p.codec.readTree<JsonNode>(p)
        val email = node["email"]?.asText() ?: throw JsonParseException("missing email field")
        val username = node["username"]?.asText() ?: throw JsonParseException("missing username field")
        val isAdmin = node["isAdmin"]?.asBoolean() ?: false
        val isBlocked = node["isBlocked"]?.asBoolean() ?: false
        return User.of(email, username, isAdmin, isBlocked)
    }
}
