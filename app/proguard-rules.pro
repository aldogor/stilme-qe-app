# ==============================================================================
# ProGuard rules for STILME-QE-APP
# ==============================================================================

# Keep line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ==============================================================================
# Retrofit
# ==============================================================================
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Keep Retrofit API interfaces
-keep,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Retrofit does reflection on generic parameters
-keepattributes Exceptions

# Keep generic type info for Retrofit
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Retrofit does reflection on method and parameter annotations
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep the RedcapApiService interface
-keep interface com.aldogor.stilme_qe_app.network.RedcapApiService { *; }

# ==============================================================================
# OkHttp
# ==============================================================================
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ==============================================================================
# Gson
# ==============================================================================
-keepattributes Signature
-keepattributes *Annotation*

# Keep Gson SerializedName annotations
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep all data classes used with Gson serialization
-keep class com.aldogor.stilme_qe_app.study.ParticipantState { *; }
-keep class com.aldogor.stilme_qe_app.study.ConsentData { *; }
-keep class com.aldogor.stilme_qe_app.study.ScaleScores { *; }
-keep class com.aldogor.stilme_qe_app.DailyUsage { *; }
-keep class com.aldogor.stilme_qe_app.AppUsageData { *; }
-keep class com.aldogor.stilme_qe_app.update.UpdateChecker$UpdateInfo { *; }

# ==============================================================================
# Room
# ==============================================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ==============================================================================
# AndroidX Security Crypto / Tink
# ==============================================================================
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

# Tink references optional dependencies (KeysDownloader) that we don't use
-dontwarn com.google.api.client.http.**
-dontwarn com.google.api.client.http.javanet.**
-dontwarn org.joda.time.**

# ==============================================================================
# Kotlin Enums (used for JSON serialization and study logic)
# ==============================================================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep study enums explicitly
-keep enum com.aldogor.stilme_qe_app.study.Timepoint { *; }
-keep enum com.aldogor.stilme_qe_app.study.StudyGroup { *; }

# ==============================================================================
# BuildConfig
# ==============================================================================
-keep class com.aldogor.stilme_qe_app.BuildConfig { *; }

# ==============================================================================
# WorkManager
# ==============================================================================
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ==============================================================================
# Kotlin Coroutines
# ==============================================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ==============================================================================
# Kotlin Serialization (if used in the future)
# ==============================================================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
