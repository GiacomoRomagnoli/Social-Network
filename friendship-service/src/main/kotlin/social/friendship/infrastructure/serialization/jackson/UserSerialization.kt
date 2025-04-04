package social.friendship.social.friendship.infrastructure.serialization.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import social.friendship.domain.User

object UserSerializer : JsonSerializer<User>() {
    override fun serialize(entity: User, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(entity.id.value)
    }
}

object UserDeserializer : JsonDeserializer<User>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): User {
        val id = p.valueAsString
        if (id.isNullOrBlank()) throw JsonParseException("empty string")
        return User.of(id)
    }
}
