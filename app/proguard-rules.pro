# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# LiteRT-LM (com.google.ai.edge.litertlm) — native bridge через JNI.
#
# R8 не должен переименовывать классы и методы, потому что JNI-стороне
# нужны точные имена для биндинга (`Engine`, `Conversation`, `Message`,
# `Backend`, sealed-классы Content/Contents и т.п.). Без этих правил
# release-сборка падает на `initialize()` с `UnsatisfiedLinkError`.
# ---------------------------------------------------------------------------
-keep class com.google.ai.edge.litertlm.** { *; }
-keepclassmembers class com.google.ai.edge.litertlm.** { *; }
# Gson используется внутри LiteRT-LM для сериализации Content/Contents/Tools.
# Артефакт тянет его транзитивно; без keep на TypeAdapterFactory/SerializedName
# можно получить регрессии после R8 (`@Keep` на самом артефакте может быть
# неполным).
-keep class com.google.gson.** { *; }

# ---------------------------------------------------------------------------
# SQLCipher — JNI + SupportOpenHelperFactory для Room.
# ---------------------------------------------------------------------------
-keep class net.zetetic.** { *; }
-keep class net.sqlcipher.** { *; }
-dontwarn net.zetetic.**

# ---------------------------------------------------------------------------
# Room — сущности и DAO не должны переименовываться (имена таблиц/колонок).
# ---------------------------------------------------------------------------
-keep @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# ---------------------------------------------------------------------------
# kotlinx.serialization — DTO зашифрованного бэкапа (.pocketreflect payload).
# ---------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}
-keep @kotlinx.serialization.Serializable class com.pocketreflect.app.data.transfer.** { *; }

# ---------------------------------------------------------------------------
# Hilt / Dagger generated entry points (стандартный набор для release).
# ---------------------------------------------------------------------------
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.EntryPoint class * { *; }