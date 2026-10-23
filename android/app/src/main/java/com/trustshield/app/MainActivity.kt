package com.trustshield.app

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.trustshield.app.screening.TrustShieldNotificationListener

class MainActivity : ComponentActivity() {
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TrustShieldScreen(resumeTick) }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrustShieldScreen(refreshTick: Int) {
    val context = LocalContext.current
    var showDisclosure by remember { mutableStateOf(false) }
    val enabled = remember(refreshTick) { isNotificationAccessEnabled(context) }
    val supabaseConfigured = BuildConfig.SUPABASE_URL.startsWith("https://") &&
        !BuildConfig.SUPABASE_URL.contains("example.invalid") &&
        BuildConfig.SUPABASE_ANON_KEY != "not-configured"

    MaterialTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("TrustShield") }) }) { padding ->
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Automatic scam protection", style = MaterialTheme.typography.headlineSmall)
                Text("TrustShield checks supported message notifications in the background. No copying or pasting is required.")
                Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Automatic message protection", style = MaterialTheme.typography.titleMedium)
                    Text(if (enabled) "Notification access is enabled." else "Enable notification access to let TrustShield inspect supported message notifications.")
                    Text("Supported apps currently include Google Messages, WhatsApp, Telegram, Messenger, Viber, and Gmail. Android may hide message previews, and not every message can be inspected.")
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        if (!enabled) showDisclosure = true
                        else context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }) { Text(if (enabled) "Manage notification access" else "Enable automatic detection") }
                } }
                Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AI service", style = MaterialTheme.typography.titleMedium)
                    Text(if (supabaseConfigured) "Supabase configuration is present. The deployed Edge Function and OpenAI secret must also be configured." else "Supabase is not configured in this build yet.")
                    Text("Only messages matching local suspicious-content signals are sent for AI review. Common one-time codes and password values are redacted first.")
                } }
                Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Caller protection", style = MaterialTheme.typography.titleMedium)
                    Text("Android call-screening can be enabled separately. The current starter allows calls by default; live caller reputation is not yet connected.")
                    Button(modifier = Modifier.fillMaxWidth(), onClick = { requestCallScreeningRole(context) }) { Text("Enable call screening") }
                } }
                Text("AI assessments can be wrong. A low-risk result is not proof that a message or caller is safe.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text("Enable automatic message checks?") },
            text = { Text("Android notification access can expose notification content from apps you allow. TrustShield processes supported messaging notifications on your device and sends only messages matching local scam signals to your configured Supabase function for AI analysis. Common one-time codes and password values are redacted, but other personal information may still be present. You can disable access at any time in Android Settings.") },
            confirmButton = {
                TextButton(onClick = {
                    showDisclosure = false
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }) { Text("Continue to Settings") }
            },
            dismissButton = { TextButton(onClick = { showDisclosure = false }) { Text("Cancel") } }
        )
    }
}

private fun isNotificationAccessEnabled(context: Context): Boolean {
    val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
    val expected = android.content.ComponentName(context, TrustShieldNotificationListener::class.java)
    return enabledListeners.split(":").any { TextUtils.equals(it, expected.flattenToString()) }
}

private fun requestCallScreeningRole(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) && !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            (context as? Activity)?.startActivityForResult(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), 741)
        }
    }
}
