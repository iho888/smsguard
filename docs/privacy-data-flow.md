# Privacy data-flow specification

This document is the **load-bearing privacy contract** for SMSGuard. Every code change that touches data flow must be checked against this document. App Store and Play Console reviewers will be shown this document. The Hong Kong Personal Data (Privacy) Ordinance (PDPO) is the gating regulation.

If a code change conflicts with this document, the document wins until the document is updated. Updates require explicit review.

## TL;DR

Raw SMS body content **never leaves the device**. Period. Not in network requests, not in crash reports, not in analytics, not in support logs. Anything that ships off-device is hashed, bucketed, or boolean.

## Data classes

| Class | Examples | Egress allowed? |
| --- | --- | --- |
| **Raw SMS content** | message body, full sender ID string, attached MMS data | **Never** |
| **Derived hashes** | SHA-256 of sender ID with rotating salt; SHA-256 of each URL; SHA-256 of each registrable domain | Yes, in user-initiated reports only (Phase 3+) |
| **Coarse features** | message length bucket; boolean "has urgency phrase"; boolean "mentions currency"; boolean "matches one of N known scam-phrase categories" — never the words themselves | Yes, in user-initiated reports only |
| **Verdict telemetry** | the local detector verdict label and rule IDs that fired (no message content) | Yes, opt-in only, off by default |
| **Device attestation** | DeviceCheck token (iOS), Play Integrity token (Android) | Yes, with each report |
| **App diagnostics** | crash reports, performance traces — **must be configured to scrub message text** | Yes, only after scrubbing audit |

## Lifecycle: incoming SMS

1. **iOS**: `ILMessageFilterExtension.handle(_:context:completion:)` receives the message from iOS. The body is passed to the on-device detector.
2. **Android**: `BroadcastReceiver` for `SMS_RECEIVED` reads the body from the intent (we hold `RECEIVE_SMS`, not `READ_SMS`). The body is passed to the on-device detector.
3. The detector runs entirely in-process. Inputs: raw body. Outputs: a `Verdict` struct (label + rule IDs + explanation key).
4. The body is **discarded from memory** after the detector returns. No persistent local copy is written by SMSGuard. (The OS keeps its own copy in the system Messages app database; SMSGuard does not duplicate it.)
5. The verdict and a stable per-message ID are written to a shared App Group container (iOS) or a Room database (Android) so the main app can show "recent flags". This local store contains: verdict label, timestamp, rule IDs that fired, the *first 30 chars of the URL host* (not the URL path, not query), and a hash of the sender ID. **It does not contain the message body.**
6. The local store has a max retention of 30 days, enforced by a periodic prune job. The user can clear it instantly via Settings.

## Lifecycle: user-initiated report (Phase 3+)

When the user taps "Report this as a scam":

1. The app shows a confirmation screen explaining exactly what will be transmitted. The user must tap "Send report".
2. The app derives, in this order:
   - SHA-256 of the sender ID, salted with the current weekly salt (the salt is published in the app bundle and rotates every Monday 00:00 HKT).
   - For each URL in the body: SHA-256 of the full URL, and SHA-256 of the registrable domain (eTLD+1).
   - The coarse content-feature vector: `{ lengthBucket: enum, hasUrgency: bool, hasCurrency: bool, hasCryptoWallet: bool, scamPhraseCategories: number[] }`. The categories are integer IDs into a published list — not the matched words.
3. The app obtains a fresh DeviceCheck (iOS) or Play Integrity (Android) attestation token.
4. The app POSTs to `https://api.smsguard.hk/v1/reports` with TLS 1.3, certificate-pinned, request body containing only the items in step 2 + 3 plus a v1 schema version field.
5. The server responds with an opaque report receipt ID. The app stores the receipt ID locally so the user can later request deletion.

The app **must not** send: any portion of the message body as a string, any URL in plaintext, any sender ID in plaintext, any user contact information, any device identifier other than the platform attestation token.

## Lifecycle: blocklist update (Phase 0+)

This flow is read-only from the device's perspective.

1. The app fetches `https://blocklist.smsguard.hk/v1/bundle.json` (or its delta `bundle-delta-{from}-{to}.json`).
2. The app verifies the bundle's Ed25519 signature against a public key embedded in the app bundle.
3. If the signature checks out, the app installs the bundle as the current active blocklist.
4. **No request body** is sent in this flow — the bundle ID is in the URL, and standard HTTP request metadata (User-Agent, etc.) is the only outbound information.

## Server-side storage (Phase 3+)

The server stores:

- **Reports table**: `{ report_id, schema_version, hashed_sender_id, hashed_urls[], hashed_domains[], feature_vector, attestation_verdict, received_at }`. Retention: **90 days max**. After 90 days, individual report rows are deleted; aggregates persist.
- **Aggregated blocklist table**: `{ entry_id, kind: 'sender'|'domain'|'url', hash, first_seen_at, last_seen_at, source: 'ofca'|'scameter'|'urlhaus'|'community', confidence, status: 'shadow'|'active'|'retracted' }`. No retention limit; entries are **never linked back to individual reporters**.
- **Reporter reputation table**: `{ pseudonymous_device_id, account_age_days, agreement_score, daily_report_count, last_seen_at }`. The `pseudonymous_device_id` is derived from the platform attestation in a way that cannot be reversed to a device. Retention: 365 days from last activity.

The server **does not store** any plaintext sender IDs, any plaintext URLs, any plaintext message content, any user PII, IP addresses (Cloudflare access logs are stripped after 7 days; we do not export them), or device fingerprints.

## What we do NOT do

- We do not ingest the user's contact list, address book, location, or call log.
- We do not run behavioral analytics on user interaction with the app body — only on app-lifecycle events (open, settings change, report sent) and only opt-in.
- We do not offer cloud backup of the local flag history.
- We do not share, sell, or rent any data to third parties.
- We do not honor "list of bad numbers" requests from third parties — only the OFCA SSRS, HKMA bank list, gov.hk, and HKPF Scameter feeds drive the allowlist/blocklist.

## Data subject rights

A Hong Kong data subject under PDPO has rights of access and correction. The app implements:

- **Access**: in-app "Show my data" screen lists every receipt ID the app has issued and the local flag history. Tapping an entry shows the exact bytes that were transmitted, reconstructed from the receipt ID.
- **Correction / withdrawal**: in-app "Delete my reports" issues a `DELETE /v1/reports/by-receipt/{receipt_id}` for each receipt. The server soft-deletes the row immediately and hard-deletes within 24 hours. The aggregated blocklist entries are **not** retracted — they are aggregates and not personally identifiable, so the right of correction does not extend to them under PDPO DPP 2.

## Audit hooks (mandatory)

Two automated checks must pass on every PR before merge:

1. **No-body-egress test**: a network-recording test that captures every outbound HTTP request the app makes during a scripted scenario (boot, receive 5 sample SMS including 2 flagged, report 1, fetch blocklist). The test fails if any request body or URL contains any substring of the sample SMS text.
2. **Hash-only schema test**: a JSON-schema validator on the report API request body that rejects any field not in the allowlist of hashed/feature fields.

If either check fails, the build fails. These are non-overridable.

## Open questions, to be resolved before public launch

- Final decision on whether app diagnostics use Apple's BugSnag-equivalent or a self-hosted alternative; the third-party choice must support body-scrubbing.
- Whether to publish a quarterly transparency report listing aggregate counts (number of reports received, number of blocklist entries promoted, number of takedown requests honored). Recommendation: yes.
