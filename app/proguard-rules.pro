# Jetpack Compose rules
-keep class androidx.compose.ui.platform.** { *; }
-keepclassmembers class * extends androidx.compose.ui.node.LayoutNode { *; }

# Room rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# WorkManager & Coroutines
-keep class androidx.work.** { *; }
-keep class kotlinx.coroutines.** { *; }
