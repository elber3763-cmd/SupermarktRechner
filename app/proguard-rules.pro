# ProGuard rules for EinkaufsScanner

# Keep all public classes and methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep OpenCV classes
-keep class org.opencv.** { *; }

# Keep ML Kit classes
-keep class com.google.mlkit.** { *; }

# Keep Hilt generated code
-keep class **_Factory { *; }
-keep class **_Provide* { *; }
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }

# Keep Kotlin coroutines
-keepclasseswithmembers class kotlinx.coroutines.** {
    *;
}

# Keep Jetpack Compose classes
-keep class androidx.compose.** { *; }

# Keep Android Architecture Components
-keep class androidx.lifecycle.** { *; }
-keep class androidx.room.** { *; }

# Keep our app classes
-keep class com.einkaufsscanner.** { *; }

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep serialization classes
-keepclassmembers class com.einkaufsscanner.** {
    *;
}

# Optimization settings
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
