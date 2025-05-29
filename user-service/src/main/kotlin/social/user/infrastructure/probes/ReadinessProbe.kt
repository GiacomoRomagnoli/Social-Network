package social.user.infrastructure.probes

import io.vertx.core.Future
import io.vertx.core.Vertx
import java.util.concurrent.Callable

class ReadinessProbe(
    private val syncProbes: Iterable<SyncProbe>,
    private val asyncProbes: Iterable<AsyncProbe>
) {
    fun isReady(vertx: Vertx): Future<Unit> {
        val probes =
            syncProbes.map { vertx.executeBlocking(Callable { it.isReady() }) } + asyncProbes.map { it.isReady() }
        return Future.all(probes).mapEmpty()
    }
}
