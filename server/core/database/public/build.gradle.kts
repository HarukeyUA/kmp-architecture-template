plugins { alias(libs.plugins.convention.server.core.public) }

dependencies {
    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    api(libs.exposed.kotlin.datetime)
}
