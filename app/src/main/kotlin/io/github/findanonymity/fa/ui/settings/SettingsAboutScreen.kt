package io.github.findanonymity.fa.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.findanonymity.fa.FaApp
import io.github.findanonymity.fa.R
import io.github.findanonymity.fa.data.model.BackendPreference
import io.github.findanonymity.fa.ui.components.FormContainer
import io.github.findanonymity.fa.ui.components.TerminalCard
import io.github.findanonymity.fa.ui.theme.CorpoYellow

private const val GITHUB_URL = "https://github.com/eVersor-HN/FindAnonymity"
private const val RELEASES_URL = "https://github.com/eVersor-HN/FindAnonymity/releases/latest"
private const val KOFI_URL = "https://ko-fi.com/eversorhn"

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAboutScreen(
    onBack: () -> Unit,
    onOpenFaq: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onOpenPermissions: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val app = context.applicationContext as FaApp
    val config by viewModel.configFlow.collectAsStateWithLifecycle()

    var langExpanded by remember { mutableStateOf(false) }
    val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore("-")
        .ifBlank { "en" }
    val currentLanguage = SUPPORTED_LANGUAGES.firstOrNull { it.tag == currentTag } ?: SUPPORTED_LANGUAGES.first()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
                Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleSmall)
                ExposedDropdownMenuBox(
                    expanded = langExpanded,
                    onExpandedChange = { langExpanded = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    OutlinedTextField(
                        value = currentLanguage.nativeName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = langExpanded,
                        onDismissRequest = { langExpanded = false },
                    ) {
                        SUPPORTED_LANGUAGES.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(language.nativeName) },
                                onClick = {
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.forLanguageTags(language.tag),
                                    )
                                    langExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            TerminalCard(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_backend_preference), style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    BackendPreference.entries.forEachIndexed { index, preference ->
                        SegmentedButton(
                            selected = config.preferredBackend == preference,
                            onClick = {
                                viewModel.setBackendPreference(preference)
                                app.executorManager.preference = preference
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, BackendPreference.entries.size),
                        ) {
                            Text(stringResource(preference.labelRes), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            TerminalCard(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onOpenFaq, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_help_faq))
                }
                Button(onClick = onOpenPermissions, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(stringResource(R.string.settings_permissions))
                }
                Button(onClick = onReplayOnboarding, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(stringResource(R.string.settings_onboarding_replay))
                }
            }

            TerminalCard(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_updates_section), style = MaterialTheme.typography.titleSmall)
                Text(
                    "${stringResource(R.string.settings_version)}: ${io.github.findanonymity.fa.BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    stringResource(R.string.settings_update_hint),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Button(
                    onClick = { openUrl(context, RELEASES_URL) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.settings_check_updates))
                }
            }

            TerminalCard(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_about_section), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.settings_about_author),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CorpoYellow,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "${stringResource(R.string.settings_license)}: GPLv3",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    stringResource(R.string.settings_no_internet_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(
                    onClick = { openUrl(context, GITHUB_URL) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.settings_github))
                }
                Button(
                    onClick = { openUrl(context, KOFI_URL) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.settings_support))
                }
            }
        }
    }
}
