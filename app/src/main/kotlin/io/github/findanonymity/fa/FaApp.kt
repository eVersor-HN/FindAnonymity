package io.github.findanonymity.fa

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.github.findanonymity.fa.core.exec.PrivilegedExecutorManager
import io.github.findanonymity.fa.data.AppConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class FaApp : Application() {

    val appScope = CoroutineScope(SupervisorJob())
    lateinit var configRepository: AppConfigRepository
        private set
    lateinit var executorManager: PrivilegedExecutorManager
        private set

    override fun onCreate() {
        super.onCreate()

        // Default the app to English until the user explicitly picks a language in Settings,
        // rather than following the system locale.
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }

        configRepository = AppConfigRepository(this)
        executorManager = PrivilegedExecutorManager(this)

        val onBinderReceived = Shizuku.OnBinderReceivedListener {
            appScope.launch { executorManager.refreshState() }
        }
        val onBinderDead = Shizuku.OnBinderDeadListener {
            appScope.launch { executorManager.refreshState() }
        }
        Shizuku.addBinderReceivedListenerSticky(onBinderReceived)
        Shizuku.addBinderDeadListener(onBinderDead)

        appScope.launch { executorManager.refreshState() }

        appScope.launch {
            configRepository.configFlow
                .map { it.preferredBackend }
                .distinctUntilChanged()
                .collect {
                    executorManager.preference = it
                    executorManager.refreshState() // effective backend depends on the preference
                }
        }
    }
}
