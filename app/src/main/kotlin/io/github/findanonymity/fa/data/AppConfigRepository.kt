package io.github.findanonymity.fa.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.findanonymity.fa.data.model.AppConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "fa_config")

/**
 * Single source of truth for [AppConfig], read by both the UI and [io.github.findanonymity.fa.service.AutomationService].
 */
class AppConfigRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val configKey = stringPreferencesKey("app_config_json")

    val configFlow: Flow<AppConfig> = context.dataStore.data.map { prefs ->
        decode(prefs[configKey])
    }

    suspend fun update(transform: (AppConfig) -> AppConfig) {
        context.dataStore.edit { prefs ->
            val current = decode(prefs[configKey])
            val next = transform(current)
            prefs[configKey] = json.encodeToString(AppConfig.serializer(), next)
            BootFlagMirror.write(context, next.masterAutomationEnabled)
        }
    }

    private fun decode(raw: String?): AppConfig {
        if (raw == null) return AppConfig()
        return try {
            json.decodeFromString(AppConfig.serializer(), raw)
        } catch (e: Exception) {
            // SerializationException on schema drift, IllegalArgumentException on malformed JSON:
            // either way fall back to defaults rather than crashing the service/UI on read.
            AppConfig()
        }
    }
}

/**
 * Mirrors [AppConfig.masterAutomationEnabled] into plain SharedPreferences so [io.github.findanonymity.fa.service.BootReceiver]
 * can read it synchronously without blocking on the DataStore Flow inside the short onReceive() budget.
 */
object BootFlagMirror {
    private const val PREFS_NAME = "fa_boot_flag"
    private const val KEY_AUTOMATION_ENABLED = "automation_enabled"

    fun write(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTOMATION_ENABLED, enabled)
            .apply()
    }

    fun read(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTOMATION_ENABLED, false)
    }
}
