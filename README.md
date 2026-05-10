# SMSGuard

A free SMS spam and fraud detector for Hong Kong. iOS and Android.

The product wedge: SMSGuard knows about Hong Kong's SMS Sender Registration Scheme (SSRS, the `#prefix` carrier verification) and integrates with the HK Police Scameter database. When a message claims to be from a bank but lacks the `#bank` prefix the carrier should have verified, SMSGuard flags it.

## Repo layout

| Path | What lives here |
| --- | --- |
| `docs/` | Privacy data-flow spec, threat model, blocklist bundle format. Read these first. |
| `shared/rules/` | TypeScript detection rules. Code-generated into Swift and Kotlin at build time so iOS and Android can't drift. |
| `ios/` | Swift app + `ILMessageFilterExtension`. (Phase 1) |
| `android/` | Kotlin app, `RECEIVE_SMS` + `NotificationListenerService` posture. Does **not** replace the default SMS app. (Phase 2) |
| `services/api/` | Cloudflare Workers edge service for blocklist distribution and report ingestion. (Phase 3) |
| `tools/ssrs-scraper/` | Daily sync of the OFCA SSRS registry. |
| `tools/blocklist-builder/` | Builds and signs the blocklist bundle from all upstream feeds. |

## Status

Phase 0 — foundations. No app code yet. The detection rules in `shared/rules/` are the first runnable code.

## Privacy posture (one-line summary)

Raw SMS bodies never leave the device. See `docs/privacy-data-flow.md` for the full spec.
