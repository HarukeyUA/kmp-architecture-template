package org.example.project.server.observability

/**
 * Configuration for the Prometheus scrape endpoint exposed by `MetricsRoute`.
 *
 * The endpoint is served on its own [port], separate from the public application port, so it can be
 * kept off the public network surface: no public domain/ingress is mapped to this port, and an
 * in-network Prometheus scrapes it directly. The route rejects any request that arrives on a
 * different port, so even if `/metrics` is probed on the public port it stays invisible (404).
 */
data class MetricsConfig(val port: Int)
