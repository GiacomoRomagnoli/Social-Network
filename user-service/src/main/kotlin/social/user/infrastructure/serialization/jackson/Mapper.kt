package social.user.infrastructure.serialization.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import social.user.domain.Credentials
import social.user.domain.User

object Mapper : ObjectMapper() {
    private fun readResolve(): Any = Mapper
    init {
        registerModule(KotlinModule.Builder().build())
        registerModule(
            SimpleModule().apply {
                addSerializer(User::class.java, UserSerializer)
                addDeserializer(User::class.java, UserDeserializer)
                addDeserializer(Credentials::class.java, CredentialDeserializer)
            }
        )
        configure(SerializationFeature.INDENT_OUTPUT, true)
    }
}
