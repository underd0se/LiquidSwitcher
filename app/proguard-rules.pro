# Proguard rules for TV App Switcher

# Keep data models used by Gson serialization
-keepclassmembers class com.baris.tvappswitcher.AppInfo { *; }
-keep class com.baris.tvappswitcher.AppInfo { *; }
-keepclassmembers class com.baris.tvappswitcher.ConfigManager** { *; }
-keep class com.baris.tvappswitcher.ConfigManager** { *; }

# Keep Custom Views instantiated via XML
-keep class com.baris.tvappswitcher.LiquidDockView { *; }
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep Services and BroadcastReceivers
-keep class com.baris.tvappswitcher.KeyInterceptorService { *; }
-keep class com.baris.tvappswitcher.BootReceiver { *; }

# Gson rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
