# GrayGate ProGuard Rules

# Keep Room entities and DAOs
-keep class com.graygate.data.model.** { *; }
-keep class com.graygate.data.db.** { *; }

# Keep accessibility service
-keep class com.graygate.service.GrayGateAccessibilityService { *; }

# Keep boot receiver
-keep class com.graygate.receiver.BootReceiver { *; }
-keep class com.graygate.receiver.PackageChangeReceiver { *; }

# Keep tile service
-keep class com.graygate.service.GrayscaleTileService { *; }

# Strip debug and verbose logging from release builds
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
