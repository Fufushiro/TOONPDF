# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Project-specific ProGuard/R8 rules

# OPTIMIZACIONES AGRESIVAS
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
-allowaccessmodification
-repackageclasses ''

# Optimizaciones de código
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Keep solo las clases de modelos y entidades necesarias (ser más selectivo)
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Room database - solo lo esencial
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Room DAO methods
-keepclassmembers @androidx.room.Dao class * {
    @androidx.room.* <methods>;
}

# Gson - solo atributos necesarios
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.google.gson.**
# Keep solo clases de datos necesarias (en lugar de todo el paquete)
-keepclassmembers class ia.ankherth.grease.data.model.** {
    <fields>;
}
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Kotlin coroutines - solo lo necesario
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.flow.**

# Material Components - solo lo usado
-dontwarn com.google.android.material.**
-keep class com.google.android.material.** { *; }

# AndroidX - ser más selectivo
-dontwarn androidx.**
-keep class androidx.lifecycle.** { *; }
-keep class androidx.room.** { *; }

# PDF Viewer library
-keep class com.github.barteksc.pdfviewer.** { *; }
-keep class com.shockwave.** { *; }
-dontwarn com.shockwave.**

# Optimize - remove ALL logging in release (más agresivo)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Remove debug y verbose de Kotlin
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ViewBinding - solo lo esencial
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
    public static * inflate(android.view.LayoutInflater);
    public static * bind(android.view.View);
}

# Eliminar código no usado de forma agresiva
-assumenosideeffects class java.io.PrintStream {
    public void println(%);
    public void println(**);
}

# Optimizaciones de Kotlin
-dontwarn kotlin.**
-dontwarn kotlinx.**
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# Eliminar metadata innecesario de Kotlin
-dontnote kotlin.internal.PlatformImplementationsKt
-dontnote kotlin.jvm.internal.**
-dontwarn kotlin.jvm.internal.**
