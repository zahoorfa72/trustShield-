package com.trustshield.app

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.os.Bundle
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TrustShieldScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrustShieldScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("Submit a suspicious message for an assessment.") }
    var busy by remember { mutableStateOf(false) }
    MaterialTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("TrustShield") }) }) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Stay alert. Check before you trust.", style = MaterialTheme.typography.headlineSmall)
                Text("AI-assisted scam awareness. Results can be wrong. Never share passwords, PINs or one-time codes.")
                Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Scan a message", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = message, onValueChange = { message = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Paste suspicious text") }, minLines = 4)
                    Button(enabled = !busy && message.isNotBlank(), modifier = Modifier.fillMaxWidth(), onClick = {
                        busy = true
                        result = "Analyzing…"
                        val submitted = message
                        scope.launch { result = withContext(Dispatchers.IO) { analyzeText(submitted) }; busy = false }
                    }) { Text(if (busy) "Analyzing…" else "Analyze with AI") }
                } }
                Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Caller protection", style = MaterialTheme.typography.titleMedium)
                    Text("Optional Android call-screening role. The prototype allows calls by default and does not record calls.")
                    Button(modifier = Modifier.fillMaxWidth(), onClick = { requestCallScreeningRole(context) }) { Text("Enable call screening") }
                } }
                Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Analysis result", style = MaterialTheme.typography.titleMedium)
                    Text(result)
                } }
                Text("Privacy: text you submit is sent to your configured backend. Do not submit highly sensitive information.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun requestCallScreeningRole(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) && !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            (context as? Activity)?.startActivityForResult(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), 741)
        }
    }
}

private fun analyzeText(text: String): String {
    val base = BuildConfig.API_BASE_URL.trimEnd('/')
    if (base.contains("example.invalid")) return "Backend not configured. Deploy the API and set TRUSTSHIELD_API_BASE_URL in android/local.properties."
    var connection: HttpURLConnection? = null
    return try {
        connection = URL(base + "/v1/analyze/text").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 7000
        connection.readTimeout = 12000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.outputStream.use { it.write(JSONObject().put("text", text).toString().toByteArray(Charsets.UTF_8)) }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader().use { it.readText() }
        if (status in 200..299) {
            val json = JSONObject(body)
            "Risk: " + json.optString("risk_level", "unknown").uppercase() + "\n\n" + json.optString("summary", "No summary returned.") + "\n\nReasons: " + json.optJSONArray("reasons").toString() + "\n\nAdvice: " + json.optString("recommended_action", "Use caution.")
        } else "Analysis service returned an error. Please try again later."
    } catch (_: Exception) {
        "Could not reach the analysis service. Check your internet connection and backend URL. No conclusion was made."
    } finally { connection?.disconnect() }
}
