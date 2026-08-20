package io.github.findanonymity.fa.ui.monitor

import android.app.Application
import android.net.TrafficStats
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.findanonymity.fa.FaApp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Live egress monitor: shows what the device is actually transmitting, so the user can verify that
 * "radios off" really means silence — and see it when it doesn't.
 *
 * Byte counters come from [TrafficStats] (device-wide, no permission needed). Radio states are read
 * from Settings.Global/Secure rather than the manager APIs so no extra runtime permissions
 * (e.g. BLUETOOTH_CONNECT) are required. Live socket lists need a privileged backend.
 */
class TransmissionMonitorViewModel(application: Application) : AndroidViewModel(application) {

    data class Connection(val proto: String, val peer: String)

    data class MonitorState(
        val txRate: Long = 0,
        val rxRate: Long = 0,
        val txSession: Long = 0,
        val rxSession: Long = 0,
        val history: List<Long> = emptyList(),
        val wifiOn: Boolean = false,
        val dataOn: Boolean = false,
        val airplaneOn: Boolean = false,
        val bluetoothOn: Boolean = false,
        val locationOn: Boolean = false,
        val connections: List<Connection>? = null,
        val connectionsError: String? = null,
        val hasBackend: Boolean = false,
        val leak: Boolean = false,
    ) {
        /** No traffic-carrying radio is enabled, so nothing should be leaving the device. */
        val radiosSilent: Boolean get() = !wifiOn && !dataOn

        /**
         * Airplane mode is on, yet Wi-Fi and/or mobile data were re-enabled on top of it — Android
         * allows this, and traffic keeps flowing even though most people read "airplane mode" as
         * "everything off".
         */
        val airplaneBypassed: Boolean get() = airplaneOn && (wifiOn || dataOn)
    }

    private val app get() = getApplication<FaApp>()
    private val _state = MutableStateFlow(MonitorState())
    val state: StateFlow<MonitorState> = _state.asStateFlow()

    private var pollJob: Job? = null

    fun start() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            var lastTx = TrafficStats.getTotalTxBytes()
            var lastRx = TrafficStats.getTotalRxBytes()
            val baseTx = lastTx
            val baseRx = lastRx
            val history = ArrayDeque<Long>()
            var tick = 0

            while (true) {
                delay(1_000L)
                val tx = TrafficStats.getTotalTxBytes()
                val rx = TrafficStats.getTotalRxBytes()
                // UNSUPPORTED (-1) on odd devices: clamp so the UI shows 0 rather than nonsense.
                val txRate = (tx - lastTx).coerceAtLeast(0)
                val rxRate = (rx - lastRx).coerceAtLeast(0)
                lastTx = tx
                lastRx = rx

                history.addLast(txRate)
                while (history.size > HISTORY_SECONDS) history.removeFirst()

                val radios = readRadios()
                val silent = !radios.wifiOn && !radios.dataOn

                _state.value = _state.value.copy(
                    txRate = txRate,
                    rxRate = rxRate,
                    txSession = (tx - baseTx).coerceAtLeast(0),
                    rxSession = (rx - baseRx).coerceAtLeast(0),
                    history = history.toList(),
                    wifiOn = radios.wifiOn,
                    dataOn = radios.dataOn,
                    airplaneOn = radios.airplaneOn,
                    bluetoothOn = radios.bluetoothOn,
                    locationOn = radios.locationOn,
                    leak = silent && txRate > 0,
                )

                // Socket list is a privileged shell call — poll it far less often than the counters.
                if (tick % CONNECTION_POLL_SECONDS == 0) refreshConnections()
                tick++
            }
        }
    }

    fun stop() {
        pollJob?.cancel()
        pollJob = null
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }

    private class Radios(
        val wifiOn: Boolean,
        val dataOn: Boolean,
        val airplaneOn: Boolean,
        val bluetoothOn: Boolean,
        val locationOn: Boolean,
    )

    private fun readRadios(): Radios {
        val cr = app.contentResolver
        fun global(key: String): Boolean = Settings.Global.getInt(cr, key, 0) != 0
        val locationMode = runCatching {
            Settings.Secure.getInt(cr, Settings.Secure.LOCATION_MODE, 0)
        }.getOrDefault(0)
        return Radios(
            wifiOn = global(Settings.Global.WIFI_ON),
            dataOn = global("mobile_data"),
            airplaneOn = global(Settings.Global.AIRPLANE_MODE_ON),
            bluetoothOn = global("bluetooth_on"),
            locationOn = locationMode != 0,
        )
    }

    private suspend fun refreshConnections() {
        val manager = app.executorManager
        val backendUsable = manager.rootAvailable.value ||
            manager.shizukuAvailability.value == io.github.findanonymity.fa.core.exec.ShizukuAvailability.AVAILABLE
        if (!backendUsable) {
            _state.value = _state.value.copy(hasBackend = false, connections = null, connectionsError = null)
            return
        }
        val result = manager.exec("ss -tun state established 2>/dev/null || netstat -tun 2>/dev/null")
        if (!result.isSuccess) {
            _state.value = _state.value.copy(
                hasBackend = true,
                connections = null,
                connectionsError = result.stderr.firstOrNull() ?: "exit ${result.exitCode}",
            )
            return
        }
        _state.value = _state.value.copy(
            hasBackend = true,
            connections = parseConnections(result.stdout),
            connectionsError = null,
        )
    }

    companion object {
        private const val HISTORY_SECONDS = 60
        private const val CONNECTION_POLL_SECONDS = 5

        /** Parses `ss -tun state established` output into protocol + peer pairs. */
        fun parseConnections(lines: List<String>): List<Connection> = lines.mapNotNull { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 5) return@mapNotNull null
            val proto = parts[0].lowercase()
            if (proto != "tcp" && proto != "udp") return@mapNotNull null // skips the header row
            val peer = parts.last()
            if (peer.isBlank()) null else Connection(proto, peer)
        }
    }
}
