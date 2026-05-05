################################################################################
# FitTrack ProGuard / R8 Rules
################################################################################

# ── App data layer ────────────────────────────────────────────────────────────
-keep class com.fittrack.app.data.** { *; }
-keepattributes *Annotation*

# ── Kotlin Serialization ──────────────────────────────────────────────────────
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
# Prevent R8 from removing the serializer companion objects
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    static ** serializer();
    static ** $serializer;
}

# ── DataStore Preferences (Protobuf reflection crash fix) ─────────────────────
# R8 renames the internal `preferences_` field on the generated Protobuf class,
# causing "Field preferences_ not found" at runtime.  Keep all DataStore / Proto
# generated members to prevent field renaming.
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}
# Covers the PreferencesProto generated class specifically
-keepclassmembers class * {
    private static final ** DEFAULT_INSTANCE;
    private ** preferences_;
    private ** memoizedHashCode;
}

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── Hilt / Dagger ─────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# ── Health Connect ────────────────────────────────────────────────────────────
-keep class androidx.health.connect.** { *; }

# ── Jetpack Navigation (type-safe serializable routes) ───────────────────────
# @Serializable data objects used as nav routes must survive shrinking
-keep @kotlinx.serialization.Serializable class com.fittrack.app.** { *; }
