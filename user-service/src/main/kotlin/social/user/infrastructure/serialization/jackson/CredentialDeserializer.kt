package social.user.infrastructure.serialization.jackson

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import social.user.domain.Credentials

object CredentialDeserializer : JsonDeserializer<Credentials>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Credentials {
        val node = p.codec.readTree<JsonNode>(p)
        val email = node["email"]?.asText() ?: throw JsonParseException("missing email field")
        val password = node["password"]?.asText() ?: throw JsonParseException("missing password field")
        return Credentials.of(email, password)
    }
}
