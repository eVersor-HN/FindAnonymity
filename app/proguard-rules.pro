# ─────────────────────────────────────────────────────────────────────────────
# FA (Find Anonymity) — R8 keep rules for the release build.
# Conservative on purpose: privileged/security-critical packages are kept whole so
# shrinking can never strip a code path this coercion-defense tool depends on. The
# bulk of the size (Compose / AndroidX) is still shrunk.
# ─────────────────────────────────────────────────────────────────────────────

-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations, AnnotationDefault

# ── kotlinx.serialization ────────────────────────────────────────────────────
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-dontnote kotlinx.serialization.**

# App models are (de)serialized to/from DataStore JSON — keep them intact.
-keep @kotlinx.serialization.Serializable class io.github.findanonymity.fa.** { *; }
-keep class io.github.findanonymity.fa.data.model.** { *; }

# ── Privileged execution / panic (kept whole — correctness over size) ─────────
-keep class io.github.findanonymity.fa.core.exec.** { *; }
-keep class io.github.findanonymity.fa.panic.** { *; }

# ── Shizuku user service (instantiated by Shizuku by class name via ComponentName)
-keep class io.github.findanonymity.fa.core.exec.ShizukuUserService { *; }

# ── AIDL-generated IPC stub (package from the .aidl declaration) ──────────────
-keep class io.github.findanonymity.fa.IUserService { *; }
-keep class io.github.findanonymity.fa.IUserService$** { *; }
-keep class * extends android.os.Binder { *; }

# ── Shizuku ───────────────────────────────────────────────────────────────────
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-dontwarn rikka.shizuku.**
-dontwarn moe.shizuku.**

# ── libsu (root backend) ──────────────────────────────────────────────────────
-keep class com.topjohnwu.superuser.** { *; }
-dontwarn com.topjohnwu.superuser.**

# ── Tink / androidx.security.crypto (EncryptedSharedPreferences) ──────────────
-keep class com.google.crypto.tink.** { *; }
-keep class androidx.security.crypto.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn javax.annotation.**
