package io.github.findanonymity.fa.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.findanonymity.fa.data.model.ToggleTarget
import io.github.findanonymity.fa.ui.home.HomeScreen
import io.github.findanonymity.fa.ui.onboarding.FaqScreen
import io.github.findanonymity.fa.ui.onboarding.OnboardingScreen
import io.github.findanonymity.fa.ui.monitor.TransmissionMonitorScreen
import io.github.findanonymity.fa.ui.panic.PanicSetupScreen
import io.github.findanonymity.fa.ui.ruleeditor.BulkRuleEditorScreen
import io.github.findanonymity.fa.ui.ruleeditor.RebootRuleEditorScreen
import io.github.findanonymity.fa.ui.ruleeditor.ToggleRuleEditorScreen
import io.github.findanonymity.fa.ui.setup.PermissionsSetupScreen
import io.github.findanonymity.fa.ui.settings.SettingsAboutScreen
import io.github.findanonymity.fa.ui.theme.FaTheme

private object OnboardingPrefs {
    private const val NAME = "fa_onboarding"
    private const val KEY_SEEN = "seen"

    fun hasSeenOnboarding(context: Context): Boolean =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getBoolean(KEY_SEEN, false)

    fun markSeen(context: Context) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_SEEN, true).apply()
    }
}

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val RULE = "rule/{target}"
    const val REBOOT_RULE = "reboot_rule"
    const val BULK_RULE = "bulk_rule"
    const val PERMISSIONS = "permissions"
    const val SETTINGS = "settings"
    const val FAQ = "faq"
    const val PANIC = "panic"
    const val MONITOR = "monitor"

    fun rule(target: ToggleTarget) = "rule/${target.name}"
}

class MainActivity : AppCompatActivity() {

    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result not needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw behind the system bars so the app uses the whole screen; Scaffold still insets
        // its content, so nothing hides under the status or navigation bar.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        maybeRequestNotificationPermission()
        setContent {
            FaTheme {
                val navController = rememberNavController()
                val startDestination = if (OnboardingPrefs.hasSeenOnboarding(this)) Routes.HOME else Routes.ONBOARDING

                NavHost(navController = navController, startDestination = startDestination) {
                    composable(Routes.ONBOARDING) {
                        OnboardingScreen(onDone = {
                            OnboardingPrefs.markSeen(this@MainActivity)
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        })
                    }
                    composable(Routes.HOME) {
                        HomeScreen(
                            onEditToggleRule = { target -> navController.navigate(Routes.rule(target)) },
                            onEditRebootRule = { navController.navigate(Routes.REBOOT_RULE) },
                            onOpenBulk = { navController.navigate(Routes.BULK_RULE) },
                            onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                            onOpenPanic = { navController.navigate(Routes.PANIC) },
                            onOpenMonitor = { navController.navigate(Routes.MONITOR) },
                        )
                    }
                    composable(
                        Routes.RULE,
                        arguments = listOf(navArgument("target") { type = NavType.StringType }),
                    ) { backStackEntry ->
                        val targetName = backStackEntry.arguments?.getString("target") ?: ToggleTarget.WIFI.name
                        val target = ToggleTarget.entries.firstOrNull { it.name == targetName } ?: ToggleTarget.WIFI
                        ToggleRuleEditorScreen(target = target, onBack = { navController.popBackStack() })
                    }
                    composable(Routes.REBOOT_RULE) {
                        RebootRuleEditorScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Routes.BULK_RULE) {
                        BulkRuleEditorScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Routes.PERMISSIONS) {
                        PermissionsSetupScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Routes.SETTINGS) {
                        SettingsAboutScreen(
                            onBack = { navController.popBackStack() },
                            onOpenFaq = { navController.navigate(Routes.FAQ) },
                            onReplayOnboarding = { navController.navigate(Routes.ONBOARDING) },
                            onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                        )
                    }
                    composable(Routes.FAQ) {
                        FaqScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Routes.PANIC) {
                        PanicSetupScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Routes.MONITOR) {
                        TransmissionMonitorScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }

    /** On Android 13+ the ongoing status notification is only shown if this runtime permission is granted. */
    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
