import dev.nucleusframework.desktop.application.dsl.TargetFormat
import dev.nucleusframework.desktop.application.tasks.AbstractProguardTask
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.metro)
    alias(libs.plugins.nucleus)
    alias(libs.plugins.convention.spotless)
    alias(libs.plugins.convention.detekt)
    alias(libs.plugins.convention.module.graph.assert)
}

kotlin { jvmToolchain(libs.versions.jdk.get().toInt()) }

dependencies {
    api(project(":client:composeApp"))
    implementation(project(":client:core:buildinfo:public"))

    implementation(libs.kermit)
    implementation(libs.filekit.core)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)

    // Windowing runs on the Tao backend (Rust-native, no AWT window hierarchy) because it paints
    // the first frame before showing the window. `core-runtime` carries `SingleInstanceManager`.
    implementation(libs.nucleus.core.runtime)
    implementation(libs.nucleus.application)
    implementation(libs.nucleus.decorated.window.tao)
    implementation(libs.nucleus.decorated.window.material3)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.assertk)
}

// Lets the repo-wide `./gradlew jvmTest` reach this plain-JVM module's `test` task.
tasks.register("jvmTest") { dependsOn("test") }

// Keeps test JVM forks from registering as foreground apps on macOS.
tasks.withType<Test>().configureEach {
    systemProperty("java.awt.headless", "true")
    systemProperty("apple.awt.UIElement", "true")
}

// jpackage ships only in full JDKs, and a Gradle daemon on a JetBrains Runtime (e.g. Android
// Studio's) fails `checkRuntime` with "'jpackage' is missing". Point packaging at a full JDK via
// `-PjpackageJdkHome=...` or `JPACKAGE_JDK_HOME`. Unset keeps the daemon JVM, which suits CI.
val jpackageJdkHome: String? =
    providers
        .gradleProperty("jpackageJdkHome")
        .orElse(providers.environmentVariable("JPACKAGE_JDK_HOME"))
        .orNull

// `-PappEnv=dev` builds a parallel dev app that installs next to prod and never touches its data:
// the bundle id, name, Linux package, data directory and single-instance lock all change.
val isDevDesktop = providers.gradleProperty("appEnv").orNull == "dev"
val desktopAppId = if (isDevDesktop) "org.example.project.dev" else "org.example.project"
val desktopPackageName = if (isDevDesktop) "KotlinProject Dev" else "KotlinProject"

nucleus.application {
    mainClass = "org.example.project.MainKt"
    jpackageJdkHome?.let { javaHome = it }

    // `main.kt` reads this property. Appended after the plugin's own `-Dnucleus.app.id`, which
    // differs per OS, so last-definition-wins gives every OS the same identity.
    jvmArgs += "-Dnucleus.app.id=$desktopAppId"

    // The Tao event loop must own the macOS main thread (process thread 0), which on a regular JVM
    // requires this flag. Packaging and run tasks execute on the target OS, so it never reaches
    // Windows or Linux launchers.
    if (System.getProperty("os.name").orEmpty().lowercase().contains("mac")) {
        jvmArgs += "-XstartOnFirstThread"
    }

    nativeDistributions {
        targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
        // The app reaches JDK modules reflectively (ktor TLS via `java.net.http`) that jlink's
        // auto-detection can miss and only surface as a runtime `ClassNotFound`.
        includeAllModules = true
        // The user-facing name and, on macOS and Windows, the bundle and installer name.
        packageName = desktopPackageName
        packageVersion = "1.0.0"
        // Required by the electron-builder wrapping Nucleus uses for DEB; jpackage ignores it.
        homepage = "https://example.org"

        macOS {
            bundleID = desktopAppId
            appCategory = "public.app-category.productivity"
            minimumSystemVersion = "12.0"
            iconFile.set(project.file("icons/app.icns"))
        }
        windows { iconFile.set(project.file("icons/app.ico")) }
        linux {
            // Linux .deb names must be lowercase without spaces.
            packageName = desktopAppId
            iconFile.set(project.file("icons/app.png"))
        }
    }

    // `packageRelease*` and `runRelease` shrink and optimize the classpath before jpackage; the
    // plain `package*` tasks stay unshrunk. Never obfuscated.
    buildTypes.release.proguard {
        isEnabled = true
        obfuscate = false
        optimize = true
        joinOutputJars = false
        configurationFiles.from(project.file("proguard-rules.pro"))
    }
}

// ProGuard rewrites every class it copies but carries jar signatures through, so a signed jar fails
// its digest check at class load. The Decompose checker entry is dropped from dependency jars so
// the app's provider wins. Self-contained: script-level members break the configuration cache.
tasks.withType<AbstractProguardTask>().configureEach {
    doLast {
        val signatureEntry = Regex("META-INF/[^/]+\\.(SF|RSA|DSA|EC)")
        val checkerService =
            "META-INF/services/com.arkivanov.decompose.mainthread.MainThreadChecker"
        val mainJar = mainJarBaseName.get()

        fun stripJarEntries(jar: File, drop: (String) -> Boolean) {
            val replacement = File(jar.path + ".stripped")
            var dropped = false
            ZipInputStream(jar.inputStream().buffered()).use { input ->
                ZipOutputStream(replacement.outputStream().buffered()).use { output ->
                    generateSequence { input.nextEntry }
                        .forEach { entry ->
                            if (drop(entry.name)) {
                                dropped = true
                            } else {
                                output.putNextEntry(
                                    ZipEntry(entry.name).apply { time = entry.time },
                                )
                                input.copyTo(output)
                                output.closeEntry()
                            }
                        }
                }
            }
            if (dropped) {
                check(replacement.renameTo(jar)) { "Could not replace $jar" }
            } else {
                replacement.delete()
            }
        }

        destinationDir
            .get()
            .asFile
            .listFiles { file -> file.extension == "jar" }
            .orEmpty()
            .forEach { jar ->
                stripJarEntries(jar) { entry ->
                    entry.matches(signatureEntry) ||
                        (jar.name != mainJar && entry == checkerService)
                }
            }
    }
}
