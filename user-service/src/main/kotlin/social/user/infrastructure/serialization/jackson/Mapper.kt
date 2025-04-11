package social.user.infrastructure.serialization.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import social.common.events.BlockingEvent
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
                addSerializer(BlockingEvent::class.java, BlockingEventSerializer)
            }
        )
        configure(SerializationFeature.INDENT_OUTPUT, true)
    }
}
