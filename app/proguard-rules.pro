-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.livepickup.assistant.** { *; }
-dontwarn com.google.mlkit.**
