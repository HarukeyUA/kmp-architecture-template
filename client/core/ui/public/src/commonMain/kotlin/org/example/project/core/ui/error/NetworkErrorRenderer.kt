package org.example.project.core.ui.error

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import org.example.project.core.error.NetworkError
import org.example.project.core.ui.Res
import org.example.project.core.ui.error.ErrorRenderer.ResourceResult
import org.example.project.core.ui.error_network_connection
import org.example.project.core.ui.error_network_http
import org.example.project.core.ui.error_network_http_with_message
import org.example.project.core.ui.error_network_serialization

@Inject
@ContributesIntoSet(AppScope::class)
class NetworkErrorRenderer : ErrorRenderer<NetworkError> {
    override fun resolveResource(error: NetworkError): ResourceResult? =
        when (error) {
            is NetworkError.Http ->
                if (error.message != null) {
                    ResourceResult(
                        Res.string.error_network_http_with_message,
                        arrayOf(error.message as Any),
                    )
                } else {
                    ResourceResult(Res.string.error_network_http, arrayOf(error.code))
                }

            is NetworkError.Connection -> ResourceResult(Res.string.error_network_connection)
            is NetworkError.Serialization -> ResourceResult(Res.string.error_network_serialization)
        }
}
