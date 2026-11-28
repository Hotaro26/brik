# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# -dontobfuscate
# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
# -renamesourcefileattribute SourceFile

-dontwarn android.app.ActivityThread
-dontwarn android.app.ContextImpl
-dontwarn android.app.LoadedApk

-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }
-keep class com.rosan.dhizuku.** { *; }
-keep interface com.rosan.dhizuku.** { *; }
-keep class org.lsposed.hiddenapibypass.** { *; }
-keep class com.google.zxing.** { *; }
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }
-keep interface com.google.android.gms.** { *; }
-keep class com.hotaro.brik.data.model.** { *; }
-keepclassmembers class com.hotaro.brik.data.model.** { *; }
