package io.github.findanonymity.fa.ui.setup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.findanonymity.fa.FaApp
import io.github.findanonymity.fa.R
import io.github.findanonymity.fa.core.exec.ShizukuAvailability
import io.github.findanonymity.fa.ui.components.CorpoButton
import io.github.findanonymity.fa.ui.components.CorpoOutlinedButton
import io.github.findanonymity.fa.ui.components.FormContainer
import io.github.findanonymity.fa.ui.components.TerminalCard
import io.github.findanonymity.fa.ui.theme.CorpoAmber
import io.github.findanonymity.fa.ui.theme.CorpoCyan
import io.github.findanonymity.fa.ui.theme.CorpoYellow
import kotlinx.coroutines.launch

private fun isBatteryExempt(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as FaApp
    val rootAvailable by app.executorManager.rootAvailable.collectAsStateWithLifecycle()
    val shizukuAvail by app.executorManager.shizukuAvailability.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Battery-optimisation state can change while we're in the system settings screen, so re-read
    // it whenever this screen resumes.
    val lifecycleOwner = LocalLifecycleOwner.current
    var batteryExempt by remember { mutableStateOf(isBatteryExempt(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) batteryExempt = isBatteryExempt(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val rootReady = rootAvailable
    val shizukuReady = shizukuAvail == ShizukuAvailability.AVAILABLE

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.permissions_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        FormContainer(scaffoldPadding = padding) {
            TerminalCard(modifier = Modifier.fillMaxWidth(), accent = if (rootReady) CorpoCyan else CorpoAmber) {
                Text(stringResource(R.string.permissions_root_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(if (rootReady) R.string.permissions_root_available else R.string.permissions_root_unavailable),
                    color = if (rootReady) CorpoCyan else CorpoAmber,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (rootReady) {
                    GrantedButton()
                } else {
                    CorpoOutlinedButton(
                        text = stringResource(R.string.permissions_root_check),
                        onClick = { scope.launch { app.executorManager.checkRoot() } },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }

            TerminalCard(modifier = Modifier.fillMaxWidth(), accent = if (shizukuReady) CorpoCyan else CorpoAmber) {
                Text(stringResource(R.string.permissions_shizuku_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(
                        when (shizukuAvail) {
                            ShizukuAvailability.AVAILABLE -> R.string.permissions_shizuku_available
                            ShizukuAvailability.NEEDS_PERMISSION -> R.string.permissions_shizuku_needs_permission
                            ShizukuAvailability.UNREACHABLE -> R.string.permissions_shizuku_unreachable
                        },
                    ),
                    color = if (shizukuReady) CorpoCyan else CorpoAmber,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (shizukuReady) {
                    GrantedButton()
                } else {
                    Text(
                        stringResource(R.string.permissions_shizuku_instructions),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    CorpoButton(
                        text = stringResource(R.string.permissions_shizuku_grant),
                        onClick = { scope.launch { app.executorManager.requestShizukuPermission() } },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }

            TerminalCard(modifier = Modifier.fillMaxWidth(), accent = if (batteryExempt) CorpoCyan else CorpoAmber) {
                Text(stringResource(R.string.permissions_battery_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.permissions_battery_body),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (batteryExempt) {
                    GrantedButton()
                } else {
                    CorpoOutlinedButton(
                        text = stringResource(R.string.permissions_battery_button),
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}"),
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }

            TerminalCard(modifier = Modifier.fillMaxWidth(), accent = CorpoAmber) {
                Text(
                    stringResource(R.string.permissions_oem_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = CorpoAmber,
                )
                Text(
                    stringResource(R.string.permissions_oem_body),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun GrantedButton() {
    CorpoOutlinedButton(
        text = stringResource(R.string.permissions_granted),
        onClick = {},
        enabled = false,
        accent = CorpoCyan,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}
