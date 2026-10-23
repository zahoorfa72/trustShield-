# TrustShield Supabase setup

TrustShield uses Supabase Edge Functions as its only backend. The Edge Function calls OpenAI server-side; the OpenAI key must never be included in Android code or GitHub.

## Deploy

1. Create a Supabase project.
2. Install the Supabase CLI and log in.
3. From the repository root, run `supabase link --project-ref YOUR_PROJECT_REF`.
4. Apply the quota migration: `supabase db push`.
5. Set the OpenAI secret: `supabase secrets set OPENAI_API_KEY=YOUR_OPENAI_API_KEY`.
6. Optionally select a model: `supabase secrets set OPENAI_MODEL=gpt-4.1-mini`.
7. Deploy: `supabase functions deploy analyze-scam --no-verify-jwt`.

The Edge Function uses the server-side `SUPABASE_SERVICE_ROLE_KEY` environment variable to call the database quota function. Keep this key server-side only. The migration restricts quota table access and limits anonymous analysis to 30 requests per client IP per 60-second window. This is a baseline safeguard, not a complete abuse-prevention system; monitor usage and add stronger controls before scaling.

## Android configuration

In `android/local.properties` (do not commit this file), configure:

```properties
TRUSTSHIELD_SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
TRUSTSHIELD_SUPABASE_ANON_KEY=YOUR_SUPABASE_PUBLISHABLE_OR_ANON_KEY
```

The Android app may contain the Supabase publishable/anon key. Never put the service-role key or OpenAI key in the app. Notification access is optional and must be enabled by the user in Android Settings after the app's disclosure. The app only submits notification text that first matches local suspicious-content checks, with common one-time codes and password/PIN values redacted. Redaction is not perfect, so users must understand that message text may be sent to OpenAI for analysis.

Before public production launch, add an account/privacy flow as appropriate, quotas and monitoring beyond IP limits, data deletion controls, security review, and Google Play policy review.
