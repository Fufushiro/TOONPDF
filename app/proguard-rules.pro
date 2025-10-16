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

# Project-specific ProGuard/R8 rules

-keep class ia.ankherth.grease.** { *; }
-keepclassmembers class * implements android.os.Parcelable { public static final ** CREATOR; }

# Room keeps (DAO/Entities often accessed via reflection)
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# Gson model classes: keep fields for serialization (adjust if needed)
-keepclassmembers class ia.ankherth.grease.** { *; }
