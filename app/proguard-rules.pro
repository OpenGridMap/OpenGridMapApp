# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\IDE's\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-dontwarn com.squareup.**

-dontwarn android.support.v4.**
-keep public class com.google.android.gms.* { public *; }
-dontwarn com.google.android.gms.**

-keepnames class org.apache.** {*;}
-keep public class org.apache.** {*;}
-dontwarn org.apache.commons.logging.LogFactory
-dontwarn org.apache.http.annotation.ThreadSafe
-dontwarn org.apache.http.annotation.Immutable
-dontwarn org.apache.http.annotation.NotThreadSafe