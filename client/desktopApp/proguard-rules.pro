# Desktop (JVM) release rules, applied on top of the Nucleus defaults (Compose, coroutines,
# serialization, JNA, Nucleus JNI). Names are kept, so every keep guards a class reached by name at
# runtime; the shipped `META-INF/proguard` rules ProGuard ignores are inlined.

-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*

# Specialization narrows a type but leaves the casts in the body, which the verifier rejects at class
# load: `okio.Okio.buffer` to `RealBufferedSink`, and `runBlocking`'s block to the only lambda it is
# called with. Every other pass stays on.
-optimizations !method/specialization/returntype,!method/specialization/parametertype

# --- ServiceLoader providers ---
# Named only by a META-INF/services file, which ProGuard does not treat as a reference. A shrunk
# provider makes the loader throw ServiceConfigurationError at its first use.
-keep class * implements com.arkivanov.decompose.mainthread.MainThreadChecker { *; }
-keep class * implements kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keep class * implements io.ktor.client.HttpClientEngineContainer { *; }
-keep class * implements io.ktor.serialization.kotlinx.KotlinxSerializationExtensionProvider { *; }

# --- OkHttp (Ktor engine) ---
# Optional TLS/platform integrations OkHttp probes for and never finds on a plain JDK.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# --- Ktor ---
# AtomicFU updates these fields through AtomicReferenceFieldUpdater, i.e. by name.
-keepclassmembers class io.ktor.** {
    volatile <fields>;
}

# --- Room and bundled SQLite ---
# The native library binds these methods by name and throws SQLiteException through JNI.
-keep class * extends androidx.room3.RoomDatabase { <init>(); }
-keepclasseswithmembers class androidx.sqlite.driver.bundled.** { native <methods>; }
-keep class androidx.sqlite.SQLiteException { <init>(...); }

# --- androidx.annotation.Keep ---
-keep @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <init>(...);
}

# --- MaterialKolor ---
# Shipped consumer rule; see https://github.com/jordond/MaterialKolor/issues/493.
-keep public class com.materialkolor.*Kt { public *; }
-keep public class com.materialkolor.ktx.*Kt { public *; }

# --- dbus-java (Linux, via Nucleus dark-mode and accent-colour detection) ---
# Marshals D-Bus interfaces through java.lang.reflect proxies and resolves signal and error
# classes by name; the unix-socket transport is a ServiceLoader provider.
-keep class org.freedesktop.** { *; }
-dontwarn org.freedesktop.**
-dontwarn com.github.hypfvieh.**
