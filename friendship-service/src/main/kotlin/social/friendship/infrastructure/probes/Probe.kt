package social.friendship.infrastructure.probes

import io.vertx.core.Future

interface SyncProbe {
    fun isReady()
}

interface AsyncProbe {
    fun isReady(): Future<Unit>
}
