# TrustShield

TrustShield is an Android-first scam-awareness prototype with **automatic, opt-in message-notification checks** and Supabase Edge Functions for AI analysis. Users do not need to copy and paste message text.

## Automatic message checks

- User explicitly enables Android notification access in Settings after seeing an in-app disclosure.
- The listener currently checks notifications from Google Messages, WhatsApp, WhatsApp Business, Telegram, Facebook Messenger, Viber, and Gmail.
- A local heuristic runs first. Only notifications matching suspicious-content indicators are sent to the Supabase Edge Function.
- Common one-time codes and obvious password/PIN values are redacted before transmission, but redaction is not perfect and other personal information may remain.
- TrustShield never logs message text. Users can revoke notification access in Android Settings.
- Android may hide notification previews or not expose the full message, so this cannot inspect every message or scan old conversations.

## Supabase + OpenAI

The Android app calls `/functions/v1/analyze-scam` on Supabase. The Edge Function calls OpenAI using a server-side `OPENAI_API_KEY`. Do not put the OpenAI key or Supabase service-role key in the app or repository.

See [supabase/README.md](supabase/README.md) for setup and deployment. Configure these values in the untracked `android/local.properties` file:

```properties
TRUSTSHIELD_SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
TRUSTSHIELD_SUPABASE_ANON_KEY=YOUR_SUPABASE_PUBLISHABLE_OR_ANON_KEY
```

The public publishable/anon key is intended for client apps, but it does not protect a public Edge Function from abuse by itself. Before public production release, add authenticated access or robust rate limiting/quotas, monitoring, privacy and deletion flows, and a security review.

## Caller protection status

Android call screening is only a starter. It currently allows calls by default and is **not yet connected to live caller-reputation data**. AI message checks do not establish the real identity of a phone-number owner.

## Build

Open `android/` in Android Studio or run the GitHub Actions workflow to build a debug APK. A debug APK is for testing, not a signed Play Store release.

## Google Play and privacy

Notification access is sensitive and must be clearly disclosed and user-enabled. Google Play eligibility and policy compliance must be reviewed before release. Never treat AI results as proof that a message is safe.
