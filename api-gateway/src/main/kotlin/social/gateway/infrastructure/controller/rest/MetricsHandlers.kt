package social.gateway.infrastructure.controller.rest

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.vertx.ext.web.RoutingContext

object MetricsHandlers {
    private val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    fun prometheusGetMetrics(context: RoutingContext) {
        context.response()
            .putHeader("Content-Type", "text/plain")
            .end(prometheusRegistry.scrape())
    }

    fun counter(context: RoutingContext) {
        prometheusRegistry.counter(
            "http_requests_total",
            "path",
            context.normalizedPath()
        ).increment()
        context.next()
    }
}
