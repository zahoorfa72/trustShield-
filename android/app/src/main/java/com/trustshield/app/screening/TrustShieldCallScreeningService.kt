package com.trustshield.app.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

/** Starter only. Call decisions stay local and default to allowing calls. */
class TrustShieldCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val incoming = callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        // Safe default: allow all calls. Never wait for cloud AI in this callback.
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        if (incoming) Log.i("TrustShieldCall", "Incoming call screened; caller details omitted from logs.")
        respondToCall(callDetails, response)
    }
}
