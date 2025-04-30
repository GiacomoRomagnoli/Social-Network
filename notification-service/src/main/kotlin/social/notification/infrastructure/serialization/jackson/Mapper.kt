package social.notification.infrastructure.serialization.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import social.common.events.FriendshipRequestAccepted
import social.common.events.FriendshipRequestRejected
import social.common.events.FriendshipRequestSent
import social.common.events.MessageSent

object Mapper : ObjectMapper() {
    private fun readResolve(): Any = Mapper
    init {
        registerModule(KotlinModule.Builder().build())
        registerModule(
            SimpleModule().apply {
                addSerializer(FriendshipRequestAccepted::class.java, FriendRequestAcceptedSerializer)
                addSerializer(FriendshipRequestRejected::class.java, FriendRequestRejectedSerializer)
                addSerializer(FriendshipRequestSent::class.java, FriendRequestSentSerializer)
                addSerializer(MessageSent::class.java, MessageSentSerializer)
            }
        )
        configure(SerializationFeature.INDENT_OUTPUT, true)
    }
}
