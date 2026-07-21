plugins { alias(libs.plugins.convention.server.core.impl) }

dependencies {
    implementation(project(":server:core:lifecycle:public"))

    implementation(libs.aws.sdk.kotlin.s3)

    // The round-trip test uploads/downloads through real presigned URLs against a MinIO container.
    testImplementation(libs.testcontainers.minio)
    testImplementation(libs.ktor.client.okhttp)
}
