package io.github.findanonymity.fa.ui.setup

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.findanonymity.fa.FaApp
import io.github.findanonymity.fa.R
import io.github.findanonymity.fa.core.exec.BackendState
import io.github.findanonymity.fa.ui.components.FormContainer
import io.github.findanonymity.fa.ui.components.CorpoButton
import io.github.findanonymity.fa.ui.components.CorpoOutlinedButton
import io.github.findanonymity.fa.ui.components.TerminalCard
import io.github.findanonymity.fa.ui.theme.CorpoYellow
import io.github.findanonymity.fa.ui.theme.CorpoAmber
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as FaApp
    val backendState by app.executorManager.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

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
            TerminalCard(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.permissions_root_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(
                        if (backendState == BackendState.RootAvailable) {
                            R.string.permissions_root_available
                        } else {
                            R.string.permissions_root_unavailable
                        },
                    ),
                    color = if (backendState == BackendState.RootAvailable) CorpoYellow else CorpoAmber,
                    style = MaterialTheme.typography.bodyMedium,
                )
                CorpoOutlinedButton(
                    text = stringResource(R.string.permissions_root_check),
                    onClick = { scope.launch { app.executorManager.checkRoot() } },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }

            TerminalCard(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.permissions_shizuku_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(
                        when (backendState) {
                            BackendState.ShizukuAvailable -> R.string.permissions_shizuku_available
                            BackendState.ShizukuNeedsPermission -> R.string.permissions_shizuku_needs_permission
                            else -> R.string.permissions_shizuku_unreachable
                        },
                    ),
                    color = if (backendState == BackendState.ShizukuAvailable) CorpoYellow else CorpoAmber,
                    style = MaterialTheme.typography.bodyMedium,
                )
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

            TerminalCard(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.permissions_battery_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.permissions_battery_body),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                CorpoOutlinedButton(
                    text = stringResource(R.string.permissions_battery_button),
                    onClick = {
                        val pm = context.getSystemService(PowerManager::class.java)
                        if (pm?.isIgnoringBatteryOptimizations(context.packageName) != true) {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}"),
                                ),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }

            TerminalCard(modifier = Modifier.fillMaxWidth()) {
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
