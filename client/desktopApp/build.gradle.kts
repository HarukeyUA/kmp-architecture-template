import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.metro)
    alias(libs.plugins.convention.spotless)
    alias(libs.plugins.convention.detekt)
}

kotlin { jvmToolchain(21) }

dependencies {
    api(project(":client:composeApp"))
    implementation(project(":client:core:buildinfo:public"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
}

// `-PappEnv=dev` bakes the DEV environment into the launcher (and the `run` task): `main()` reads
// the `app.env` system property once at startup and injects the Environment into the graph.
// Defaults to prod so release packaging needs no flag; local iteration runs with
// `./gradlew -PappEnv=dev :client:desktopApp:run`. A real app would also suffix the package /
// bundle names here so a dev install can sit beside the personal prod one.
val isDevDesktop = providers.gradleProperty("appEnv").orNull == "dev"

compose.desktop {
    application {
        mainClass = "org.example.project.MainKt"

        jvmArgs += "-Dapp.env=${if (isDevDesktop) "dev" else "prod"}"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.example.project"
            packageVersion = "1.0.0"
        }
    }
}
