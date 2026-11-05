# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers for readable stack traces, and hide the original
# source file name so traces can still be retraced via the mapping file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Ktor client engine ---
# With no engine named in code, Ktor finds one through ServiceLoader. R8 only rewrites that lookup
# when it can prove the class literal; keep the container so a shrunk build still resolves OkHttp.
-keep class * implements io.ktor.client.HttpClientEngineContainer { *; }

# --- kotlinx.serialization ---
# kotlinx-serialization ships its own R8 rules, but keep the generated serializers and their
# `Companion.serializer()` accessors explicitly so DataStore / StateKeeper round-trips stay intact
# under R8 full mode.
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class ** {
    public static ** Companion;
}
-keepclassmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
