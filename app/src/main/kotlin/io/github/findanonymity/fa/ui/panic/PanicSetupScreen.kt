package io.github.findanonymity.fa.ui.panic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.findanonymity.fa.FaApp
import io.github.findanonymity.fa.R
import io.github.findanonymity.fa.core.exec.BackendState
import io.github.findanonymity.fa.panic.PanicDaemonInstaller
import io.github.findanonymity.fa.ui.components.TerminalCard
import io.github.findanonymity.fa.ui.theme.PhosphorGreen
import io.github.findanonymity.fa.ui.theme.TerminalRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanicSetupScreen(onBack: () -> Unit, viewModel: PanicViewModel = viewModel()) {
    val context = LocalContext.current
    val app = context.applicationContext as FaApp
    val backendState by app.executorManager.state.collectAsStateWithLifecycle()
    val rootAvailable = backendState == BackendState.RootAvailable

    val config by viewModel.configFlow.collectAsStateWithLifecycle()
    val pendingPassword by viewModel.pendingPassword.collectAsStateWithLifecycle()
    val armResult by viewModel.armResult.collectAsStateWithLifecycle()
    val loadedConfig = config ?: return
    val panic = loadedConfig.panicLock

    var currentCredential by remember { mutableStateOf("") }
    var passwordLength by remember { mutableStateOf(panic.passwordLength.toString()) }
    var pressCount by remember { mutableStateOf(panic.pressCount.toString()) }
    var windowSeconds by remember { mutableStateOf((panic.windowMillis / 1000).toString()) }
    var confirmText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.panic_setup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TerminalCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.panic_warning_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = TerminalRed,
                )
                Text(
                    stringResource(R.string.panic_warning_body),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            when {
                panic.armed -> {
                    TerminalCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.panic_status_armed),
                            style = MaterialTheme.typography.titleMedium,
                            color = TerminalRed,
                        )
                        Button(
                            onClick = { viewModel.disarm() },
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalRed),
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Text(stringResource(R.string.panic_disarm))
                        }
                    }
                }

                !rootAvailable -> {
                    TerminalCard(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.panic_root_required), style = MaterialTheme.typography.bodySmall)
                    }
                }

                else -> {
                    TerminalCard(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = currentCredential,
                            onValueChange = {
                                currentCredential = it
                                viewModel.setCurrentCredential(it)
                            },
                            label = { Text(stringResource(R.string.panic_current_credential_label)) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            stringResource(R.string.panic_current_credential_hint),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    TerminalCard(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = passwordLength,
                            onValueChange = { passwordLength = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.panic_password_length)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = pressCount,
                            onValueChange = { pressCount = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.panic_press_count)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        )
                        OutlinedTextField(
                            value = windowSeconds,
                            onValueChange = { windowSeconds = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.panic_window)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        )
                        Button(
                            onClick = {
                                val length = passwordLength.toIntOrNull()?.coerceAtLeast(4) ?: 100
                                val presses = pressCount.toIntOrNull()?.coerceAtLeast(2) ?: 5
                                val window = (windowSeconds.toLongOrNull()?.coerceAtLeast(1) ?: 3) * 1000
                                viewModel.updateTuning(presses, window)
                                viewModel.generateNextPassword(length)
                            },
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Text(stringResource(R.string.panic_generate_preview))
                        }
                    }

                    if (pendingPassword != null) {
                        TerminalCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(R.string.panic_next_password_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = PhosphorGreen,
                            )
                            SelectionContainer {
                                Text(
                                    pendingPassword.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }

                        TerminalCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(R.string.panic_backup_confirm_label),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            OutlinedTextField(
                                value = confirmText,
                                onValueChange = { confirmText = it },
                                label = { Text(stringResource(R.string.panic_backup_confirm_hint)) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            )
                            Button(
                                onClick = { viewModel.confirmBackupAndArm() },
                                enabled = confirmText == "CONFIRM" && currentCredential.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = TerminalRed),
                                modifier = Modifier.padding(top = 8.dp),
                            ) {
                                Text(stringResource(R.string.panic_arm))
                            }
                        }
                    }

                    when (val result = armResult) {
                        is PanicDaemonInstaller.ArmResult.CommandFailed -> {
                            Text(result.message, color = TerminalRed, style = MaterialTheme.typography.bodySmall)
                        }
                        PanicDaemonInstaller.ArmResult.NoPowerKeyDeviceFound -> {
                            Text(
                                stringResource(R.string.panic_no_power_device),
                                color = TerminalRed,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
