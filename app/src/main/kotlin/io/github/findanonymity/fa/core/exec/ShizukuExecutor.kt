package io.github.findanonymity.fa.core.exec

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import io.github.findanonymity.fa.IUserService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import rikka.shizuku.Shizuku

/**
 * Shizuku backend. Runs shell commands via a bound "user service" (Shizuku's officially
 * recommended approach) rather than the deprecated/restricted Shizuku.newProcess. The service
 * runs with uid 2000 (shell, Wireless Debugging pairing) or uid 0 (root, Shizuku running as Sui).
 */
class ShizukuExecutor(private val appContext: Context) : PrivilegedExecutor {

    private val json = Json { ignoreUnknownKeys = true }
    private var boundService: IUserService? = null
    private var connection: ServiceConnection? = null

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(appContext.packageName, ShizukuUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("privileged")
            .debuggable(false)
            .version(1)
    }

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun hasPermission(): Boolean = withContext(Dispatchers.IO) {
        try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun requestPermission(): Boolean {
        if (!isAvailable()) return false
        if (hasPermission()) return true

        val deferred = CompletableDeferred<Boolean>()
        val requestCode = REQUEST_CODE
        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode0: Int, grantResult: Int) {
                if (requestCode0 == requestCode) {
                    deferred.complete(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        try {
            Shizuku.requestPermission(requestCode)
            return withTimeoutOrNull(60_000) { deferred.await() } ?: false
        } finally {
            Shizuku.removeRequestPermissionResultListener(listener)
        }
    }

    override suspend fun exec(command: String): ShellResult {
        val service = ensureBound() ?: return ShellResult(-1, emptyList(), listOf("Shizuku-Dienst nicht gebunden"))
        return withContext(Dispatchers.IO) {
            try {
                val raw = service.exec(command)
                json.decodeFromString(ShellResult.serializer(), raw)
            } catch (e: Exception) {
                boundService = null // force rebind next time; service may have died
                ShellResult(-1, emptyList(), listOf(e.message ?: "Shizuku exec fehlgeschlagen"))
            }
        }
    }

    private suspend fun ensureBound(): IUserService? {
        boundService?.let { return it }
        if (!isAvailable() || !hasPermission()) return null

        val deferred = CompletableDeferred<IUserService?>()
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val service = binder?.let { IUserService.Stub.asInterface(it) }
                boundService = service
                if (!deferred.isCompleted) deferred.complete(service)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                boundService = null
            }
        }
        connection = conn
        Shizuku.bindUserService(userServiceArgs, conn)
        return withTimeoutOrNull(10_000) { deferred.await() }
    }

    fun unbind() {
        connection?.let { Shizuku.unbindUserService(userServiceArgs, it, true) }
        connection = null
        boundService = null
    }

    companion object {
        private const val REQUEST_CODE = 22581
    }
}
