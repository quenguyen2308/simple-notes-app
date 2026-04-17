# ============================================================
# Compose Runtime
# Fix: SnapshotStateList.conditionalUpdate failed lock verification
# ============================================================
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class androidx.compose.runtime.snapshots.** { *; }
-dontwarn androidx.compose.**

# ============================================================
# Koin DI
# Keep all ViewModel constructors so Koin can instantiate them
# ============================================================
-keep class org.koin.** { *; }
-keepclassmembers class org.koin.** { *; }
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ============================================================
# Room Database
# Keep entity/DAO/database classes and generated implementations
# ============================================================
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers @androidx.room.Dao interface * { *; }
-keep class **_Impl { *; }
-keepclassmembers class **_Impl { *; }

# ============================================================
# App Models & Data Classes (Room + Gson serialization)
# ============================================================
-keep class com.yourname.simplenotes.domain.model.** { *; }
-keep class com.yourname.simplenotes.data.local.** { *; }
-keepclassmembers class com.yourname.simplenotes.** {
    <fields>;
    <init>(...);
}

# ============================================================
# WorkManager
# ============================================================
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# ============================================================
# Biometric
# ============================================================
-keep class androidx.biometric.** { *; }
-keepclassmembers class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# ============================================================
# Bcrypt (PIN hashing)
# ============================================================
-keep class at.favre.lib.** { *; }
-dontwarn at.favre.lib.**

# ============================================================
# Security Crypto (EncryptedSharedPreferences)
# ============================================================
-keep class androidx.security.crypto.** { *; }
-keepclassmembers class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# ============================================================
# Google API Client / Drive / Sign-In
# ============================================================
-keep class com.google.api.** { *; }
-keep class com.google.apis.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.api.**
-dontwarn com.google.android.gms.**

# ============================================================
# Navigation Compose
# ============================================================
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ============================================================
# Gson
# ============================================================
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ============================================================
# Apache HTTP (transitive from google-api-client)
# ============================================================
-dontwarn javax.naming.InvalidNameException
-dontwarn javax.naming.NamingException
-dontwarn javax.naming.directory.Attribute
-dontwarn javax.naming.directory.Attributes
-dontwarn javax.naming.ldap.LdapName
-dontwarn javax.naming.ldap.Rdn
-dontwarn org.ietf.jgss.**
