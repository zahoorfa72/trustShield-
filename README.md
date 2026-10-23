# TrustShield

Android-first scam-awareness MVP: native Kotlin app, AI-assisted user-submitted message analysis, and a safe call-screening foundation.

## Important limitations
This is a development starter, not a production security product. Do not rely on it as the sole protection against scams. Unknown callers are not necessarily malicious. Basic URL checks are heuristics, not live reputation checks. Authentication, rate limiting, commercial URL threat intelligence, privacy/deletion flows, security review and broader testing are required before launch.

The call-screening service defaults to allowing calls and does not record calls. It requires the user to grant Android's call-screening role. It does not read WhatsApp messages.

## Backend
Use Python 3.11+. Install backend/requirements.txt, set OPENAI_API_KEY in a private environment (never commit it), then run uvicorn app.main:app --reload from backend/. Without an API key, heuristic analysis still works.

## Android
Open android/ in Android Studio. Set TRUSTSHIELD_API_BASE_URL in android/local.properties. Do not commit local.properties or credentials. Use HTTPS for deployed servers.

## CI
GitHub Actions builds a debug APK on Android source changes. The debug APK is a test artifact, not a Play Store release.

## Before public launch
Add authentication, quotas, rate limits, a commercial URL reputation provider, privacy and deletion flows, evaluation data, signed release configuration, and testing on multiple Android versions.