package io.github.findanonymity.fa.panic

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Holds the two secrets the panic feature needs locally: the device's current lock-screen
 * credential (required as `--old` for `locksettings set-password`) and the pre-generated
 * "next password" the user must back up externally before arming. Android Keystore-backed
 * (StrongBox where available) so these never leave the device and aren't readable by other apps.
 */
class SecureCredentialStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "fa_panic_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveCurrentCredential(value: String) {
        prefs.edit().putString(KEY_CURRENT_CREDENTIAL, value).apply()
    }

    fun getCurrentCredential(): String? = prefs.getString(KEY_CURRENT_CREDENTIAL, null)

    fun saveNextPassword(value: String) {
        prefs.edit().putString(KEY_NEXT_PASSWORD, value).apply()
    }

    fun getNextPassword(): String? = prefs.getString(KEY_NEXT_PASSWORD, null)

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_CURRENT_CREDENTIAL = "current_credential"
        private const val KEY_NEXT_PASSWORD = "next_password"
    }
}
