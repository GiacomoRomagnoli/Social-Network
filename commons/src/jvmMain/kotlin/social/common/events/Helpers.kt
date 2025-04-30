package social.common.events

import java.time.Instant

fun MessageReceived.timestamp(): Instant = Instant.parse(timestamp)
fun MessageSent.timestamp(): Instant = Instant.parse(timestamp)
