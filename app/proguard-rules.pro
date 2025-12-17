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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ============================================================================
# XArchiver ProGuard Rules
# ============================================================================

# Keep Apache Commons Compress classes
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**

# Keep XArchiver core archive classes
-keep class id.xms.xarchiver.core.archive.** { *; }
-keepclassmembers class id.xms.xarchiver.core.archive.** { *; }

# Keep coroutine continuation classes
-keepclassmembers class kotlin.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# Keep Flow classes
-keep class kotlinx.coroutines.flow.** { *; }