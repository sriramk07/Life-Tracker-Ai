# ============================================================================
# LIFEOS AI — PRODUCTION R8 / PROGUARD KEEP RULES (RELEASE 1.0)
# ============================================================================

# Preserve line numbers and source filenames for clear stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room SQLite Entities & DAOs
-keep class com.example.data.local.entities.** { *; }
-keep interface com.example.data.local.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Domain Models & Gamification Structures
-keep class com.example.domain.model.** { *; }
-keep class com.example.domain.ai.** { *; }
-keep class com.example.data.sync.** { *; }

# Moshi & Kotlin Metadata
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
}
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# OkHttp & Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Firebase Auth & Cloud Firestore
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class com.example.data.remote.** { *; }
-dontwarn com.google.firebase.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.flow.**
