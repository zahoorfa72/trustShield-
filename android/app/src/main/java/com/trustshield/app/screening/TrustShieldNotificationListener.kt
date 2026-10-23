package com.trustshield.app.screening

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.os.Build
import android.util.Log
import com.trustshield.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.LinkedHashSet
import java.util.Locale

/** Optional, user-enabled listener. It reads supported message notifications, runs local checks first,
 * redacts common one-time codes, and sends suspicious matches to Supabase. Never logs message text. */
class TrustShieldNotificationListener : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recentHashes = LinkedHashSet<String>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!SUPPORTED_PACKAGES.contains(sbn.packageName)) return
        val extras = sbn.notification.extras ?: return
        val raw = listOfNotNull(
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        ).joinToString("\n").trim()
        if (raw.length < 12 || !looksSuspicious(raw)) return
        val redacted = redactSensitiveValues(raw).take(4000)
        if (redacted.length < 12) return
        val hash = sha256(sbn.packageName + ":" + redacted)
        synchronized(recentHashes) {
            if (!recentHashes.add(hash)) return
            if (recentHashes.size > 200) {
                val iterator = recentHashes.iterator()
                if (iterator.hasNext()) { iterator.next(); iterator.remove() }
            }
        }
        serviceScope.launch {
            val result = analyzeWithSupabase(redacted, sbn.packageName)
            if (result != null) showRiskAlert(result)
        }
    }

    private fun looksSuspicious(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val signals = listOf(
            "urgent", "act now", "account suspended", "verify your account",
            "verify account", "click here", "claim your prize", "you have won",
            "payment failed", "send money", "transfer funds", "bank account",
            "blocked account", "confirm your identity", "wallet", "refund",
            "limited time", "security alert", "unusual activity", "bit.ly/",
            "tinyurl.com/", "free gift", "investment guaranteed", "crypto"
        )
        val hasLink = Regex("https?://|www\\.").containsMatchIn(lower)
        return signals.any { lower.contains(it) } ||
            (hasLink && listOf("verify", "account", "bank", "payment", "prize", "wallet").any { lower.contains(it) })
    }

    private fun redactSensitiveValues(text: String): String {
        var safe = text
        safe = Regex("(?i)(otp|one[- ]time (?:password|code)|verification code|security code|passcode)(\\s*(?:is|:|=)?\\s*)([0-9]{4,8})")
            .replace(safe, "$1$2[REDACTED]")
        safe = Regex("\\b[0-9]{6}\\b").replace(safe, "[REDACTED_CODE]")
        safe = Regex("(?i)(password|passcode|pin)(\\s*(?:is|:|=)?\\s*)\\S+")
            .replace(safe, "$1$2[REDACTED]")
        return safe
    }

    private fun analyzeWithSupabase(text: String, source: String): JSONObject? {
        val base = BuildConfig.SUPABASE_URL.trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        if (base.contains("example.invalid") || anonKey == "not-configured") return null
        var connection: HttpURLConnection? = null
        return try {
            connection = URL("$base/functions/v1/analyze-scam").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 7000
            connection.readTimeout = 18000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("apikey", anonKey)
            connection.setRequestProperty("Authorization", "Bearer $anonKey")
            val body = JSONObject().put("text", text).put("source", source).toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            if (status !in 200..299) return null
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(response)
        } catch (e: Exception) {
            Log.w("TrustShieldAuto", "Automatic analysis unavailable (${e.javaClass.simpleName})")
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun showRiskAlert(result: JSONObject) {
        val risk = result.optString("risk_level", "medium").lowercase(Locale.ROOT)
        if (risk != "high" && risk != "medium") return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "trustshield_risk_alerts"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(channelId, "Scam risk alerts", NotificationManager.IMPORTANCE_HIGH))
        }
        val summary = result.optString("summary", "A message may be suspicious.")
        val advice = result.optString("recommended_action", "Verify using an official channel.")
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        val notification = builder.setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(if (risk == "high") "TrustShield: High scam risk" else "TrustShield: Check this message")
            .setContentText(summary.take(180))
            .setStyle(Notification.BigTextStyle().bigText("$summary\n\n$advice"))
            .setAutoCancel(true)
            .build()
        try {
            manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        } catch (e: SecurityException) {
            Log.i("TrustShieldAuto", "Notification permission unavailable; alert not shown.")
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        private val SUPPORTED_PACKAGES = setOf(
            "com.google.android.apps.messaging", "com.whatsapp", "com.whatsapp.w4b",
            "org.telegram.messenger", "com.facebook.orca", "com.viber.voip", "com.google.android.gm"
        )
    }
}
