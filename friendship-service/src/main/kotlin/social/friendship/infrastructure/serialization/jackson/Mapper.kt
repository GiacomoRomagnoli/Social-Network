package social.friendship.social.friendship.infrastructure.serialization.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import social.friendship.domain.Friendship
import social.friendship.domain.FriendshipRequest
import social.friendship.domain.Message
import social.friendship.domain.User

object Mapper : ObjectMapper() {
    private fun readResolve(): Any = Mapper

    init {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
        registerModule(
            SimpleModule().apply {
                addSerializer(User::class.java, UserSerializer)
                addDeserializer(User::class.java, UserDeserializer)
                addSerializer(Message::class.java, MessageSerializer)
                addDeserializer(Message::class.java, MessageDeserializer)
                addSerializer(Friendship::class.java, FriendshipSerializer)
                addDeserializer(Friendship::class.java, FriendshipDeserializer)
                addSerializer(FriendshipRequest::class.java, FriendshipRequestSerializer)
                addDeserializer(FriendshipRequest::class.java, FriendshipRequestDeserializer)
            }
        )
        configure(SerializationFeature.INDENT_OUTPUT, true)
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
}
