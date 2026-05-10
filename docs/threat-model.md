# Threat model

This document enumerates the realistic adversaries SMSGuard must defend against, what they can do, and what mitigations are in scope. It is sized to the v1 product — not exhaustive, but covering the threats most likely to either (a) destroy user trust or (b) get us removed from the App Store / Play Store.

## Adversaries

### A1. SMS scammer (the primary adversary)

A criminal sending bulk SMS phishing to HK numbers. Goals: get a recipient to click a phishing URL, install a malicious APK, or transfer money. Capabilities: sender-ID spoofing on networks that allow it, fast-rotating short-lived URLs, locale-aware phrasing in zh-HK, bulk SMS broker accounts, occasional use of `#`-prefix sender IDs hoping users don't notice they're not registered.

**What they will try against SMSGuard**: register lookalike `#prefixes` (`#hsbc1`, `#hsbcbank`); use very-recently-registered domains; redirect through legitimate URL shorteners; vary message text to dodge regex rules; deliver via international SMS gateways that bypass HK telco network filtering.

### A2. Coordinated mass-reporter

A malicious user or automated script that sends large volumes of false-positive reports against a real sender ID — for example, mass-reporting `#hsbc` to discredit either HSBC or SMSGuard. Goals: noise the blocklist; cause false positives on legitimate banks; trigger a media incident.

### A3. Reverse engineer / abuser

Someone who reverse-engineers the app to (a) extract the rule set so the scammer can dodge it, (b) extract the salt to deanonymize hashed sender IDs in transit, or (c) forge requests to the report API.

### A4. App Store / Play Console reviewer

Not an adversary, but functionally a gatekeeper that can reject the app. Their concern: that we are harvesting SMS content. We must demonstrably prove we are not.

### A5. State-level legal request

A Hong Kong court order requesting user data. Out of scope for technical mitigation — addressed by the data minimization in `docs/privacy-data-flow.md` (the less we store, the less there is to compel). Documented here so the design intent is explicit.

## Threat scenarios and mitigations

### T1. Scammer registers a lookalike `#prefix`

OFCA does some review before granting a registered sender ID, but the registry is open to "all sectors" and visual lookalikes are possible. **Mitigation**: the SSRS verifier check for "body claims to be Bank X but sender is not `#bankX`" runs against a curated mapping from organization names to expected prefixes — not against the full registry. The mapping is maintained in `shared/rules/src/ssrs-verifier.ts` and reviewed when OFCA expands the registered-sender list.

### T2. Scammer uses a fast-rotating URL

URLs registered minutes before the SMS blast won't be on any blocklist yet. **Mitigation**: Layer B's URL extractor runs additional heuristics — registrable domain age (resolved via Cloudflare Workers WHOIS in Phase 3, not in Phase 1), TLD risk score, and known URL-shortener whitelist. Layer C's small classifier picks up the residual via length / entropy features. Phase 1 will miss some of these by design.

### T3. Mass false-positive reports against a legitimate bank (A2)

A coordinated actor reports `#hsbc` 10,000 times to break trust. **Mitigations**, all of which must be live before the report API opens:

1. **Allowlist override**: any sender on the OFCA SSRS registry is immune to crowdsourced blocking. Reports against allowlisted senders are silently discarded and increment the reporter's hostile-score.
2. **Device attestation gate**: every report requires a fresh DeviceCheck (iOS) / Play Integrity (Android) token. Reports without valid attestation are dropped.
3. **Reporter reputation weighting**: report weight = `f(account_age_days, agreement_score, daily_cap)`. New reporters carry near-zero weight until their reports agree with consensus on existing entries.
4. **Shadow quarantine**: new entries enter a 24-hour quarantine and require ≥ N independent attested reports across ≥ M hours before promotion. Defaults: N = 25, M = 6.
5. **Manual review queue** for any sender that crosses promotion threshold but matches a fuzzy-allowlist heuristic (typo of a real bank name, e.g., `#h5bc`).

### T4. Salt extraction allows hashed-sender-ID deanonymization in transit (A3)

The weekly salt is published in the app bundle. An attacker who extracts the salt can hash a list of phone numbers and check against intercepted report payloads to deanonymize reporters. **Mitigation**: the salt rotation makes intercepted payloads stale within a week, and the hash is over `sender_id`, not the recipient — so deanonymization tells the attacker "someone reported this scammer", not "this victim reported X". This is acceptable because the sender side is the scammer; the recipient is the protected party. **We do not include any recipient-side identifier in the report payload.** This is enforced by the Hash-only schema test in CI.

### T5. Forged report API requests (A3)

Without device attestation, a scripted attacker could spam the report API. **Mitigation**: server validates the DeviceCheck / Play Integrity token signature against Apple / Google's public keys on every request. Tokens are bound to the bundle ID and the request body hash to prevent replay against a different payload. Per-pseudonymous-device-id rate limit at the edge (Cloudflare). Tokens missing or invalid → 4xx and the request is not counted.

### T6. App Review rejection because reviewer believes we transmit SMS content (A4)

We must be able to demonstrate the contrary at any point. **Mitigations**:

1. The privacy data-flow doc is published in the App Store privacy nutrition label and in the Play Console data safety form, with the same wording.
2. The "no body egress" CI test is a public part of the repo.
3. The iOS extension's `Info.plist` does not declare `ILMessageFilterExtensionNetworkURL` in Phase 1 — there is no network deferral path to scrutinize.
4. The Android app does not request `READ_SMS` and does not request the `ROLE_SMS` default-app role in Phase 1, sidestepping the SMS/Call Log Permissions Declaration entirely.

### T7. Rule set extracted via reverse engineering allows scammer to dodge detection (A3)

The on-device rule set is compiled into the app and is therefore extractable. **Acceptable**: detection-rule security through obscurity is not load-bearing. The product's defense in depth is layered — SSRS check + URL blocklist + content rules + small classifier. A scammer who dodges the regex tier still trips on the SSRS-claims-bank-without-prefix rule, which depends on visible behavior (the scammer must mention a bank in the body) that they cannot eliminate without losing the scam itself. We accept rule visibility as a cost of the offline-only Phase 1 architecture.

### T8. Sybil farms (cheap attestation bypass) (A2 + A3)

Play Integrity is bypassable on rooted Android devices; DeviceCheck is hard but not impossible to spoof. **Mitigation**: device attestation is one input to the reputation score, not the sole gate. Even fully-attested devices need to build agreement-with-consensus before their reports carry weight. New attested devices are throttled to a few reports per day until they have a track record.

### T9. Compromised blocklist signing key (operational)

If the Ed25519 private key used to sign blocklist bundles leaks, an attacker can publish a malicious bundle. **Mitigations**: the key is generated and held offline (HSM or air-gapped machine). The signing process runs on a build server that fetches the key only at sign time. Keys rotate annually; the app embeds two trusted public keys (current + next) so rotation is seamless. Bundle signatures include a `valid_after` timestamp so old signatures cannot be replayed.

### T10. Server compromise leaks aggregated blocklist entries

These are by design not personally identifiable. Acceptable.

### T11. Server compromise leaks reports table

Reports contain only hashed sender IDs (with weekly-rotated salt), hashed URLs, and feature vectors. An attacker with the database and the current week's salt can dictionary-match active scam sender IDs — but those are public anyway (everyone the scammer texted has them). Personally identifying information for reporters is limited to the pseudonymous device ID, which by construction cannot be reversed to a real device. **This is the privacy-load-bearing claim** — must be re-validated on every change to the attestation handling code.

## Out of scope for v1

- iMessage, RCS Business Messaging, WhatsApp, Telegram, Signal — all use end-to-end encryption or proprietary delivery; SMSGuard does not see them.
- Voice call screening / deepfake detection.
- SIM-swap detection.
- Mainland China app distribution channels (different review regime, different compliance, different threat model).

These are deferred — not because they don't matter, but because including them would prevent any v1 from shipping.
