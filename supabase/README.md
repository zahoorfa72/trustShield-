# TrustShield Supabase setup

TrustShield uses Supabase Edge Functions as its backend. OpenAI is called only from the Edge Function; the OpenAI key must never be included in Android code or GitHub.

## Deploy

1. Create a Supabase project.
2. Install the Supabase CLI and log in.
3. From the repository root, run:
   `supabase link --project-ref YOUR_PROJECT_REF`
4. Set the server-side secret:
   `supabase secrets set OPENAI_API_KEY=YOUR_OPENAI_API_KEY`
5. Optionally select a model:
   `supabase secrets set OPENAI_MODEL=gpt-4.1-mini`
6. Deploy:
   `supabase functions deploy analyze-scam --no-verify-jwt`

The function currently accepts anonymous requests because the Android starter has no account flow yet. Before a public production launch, add authenticated users or another abuse-resistant gateway, per-user quotas/rate limits, monitoring, and a privacy/deletion policy. Never put the service-role key or OpenAI key in the app. The publishable/anon key is intended for client use and must be paired with proper server-side authorization and limits.

## Android configuration

In `android/local.properties` (do not commit this file), configure:
`TRUSTSHIELD_SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co`
`TRUSTSHIELD_SUPABASE_ANON_KEY=YOUR_SUPABASE_PUBLISHABLE_OR_ANON_KEY`

The app's automatic message analysis uses notification access only after the user enables it in Android Settings. It filters supported messaging-app notifications and only sends text that triggers a local suspicious-content heuristic. Notifications may include private information; the app displays a disclosure before asking for access. Automatic access and Play Store policy eligibility must be reviewed before publishing.
