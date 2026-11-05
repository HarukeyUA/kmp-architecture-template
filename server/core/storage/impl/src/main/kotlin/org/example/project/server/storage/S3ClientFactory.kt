package org.example.project.server.storage

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.net.url.Url

/**
 * Builds an [S3Client] for the configured (non-default) endpoint with static credentials — the one
 * place the SDK is wired, shared by the DI provider and the round-trip test so they can never
 * drift.
 */
internal fun buildS3Client(config: StorageConfig): S3Client = S3Client {
    region = config.region
    endpointUrl = Url.parse(config.endpoint)
    forcePathStyle = config.forcePathStyle
    credentialsProvider = StaticCredentialsProvider {
        accessKeyId = config.accessKey
        secretAccessKey = config.secretKey
    }
}
