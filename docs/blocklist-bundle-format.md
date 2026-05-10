# Blocklist bundle format v1

The blocklist bundle is the signed JSON file that iOS and Android apps fetch to update their detection data. It is the **only mechanism** for updating SMSGuard's data without an app-store release, so the format matters.

## Distribution

- Full bundle URL: `https://blocklist.smsguard.hk/v1/bundle.json`
- Delta bundle URL: `https://blocklist.smsguard.hk/v1/bundle-delta-{from_version}-{to_version}.json`
- Signature: detached Ed25519, base64-encoded, served with the bundle in a sibling URL: `bundle.json.sig` (or `bundle-delta-X-Y.json.sig`).
- Cache: Cloudflare edge, HKG region. `Cache-Control: public, max-age=300`. Apps poll every 6 hours when the app is open.

## Verification rules (enforced by both clients)

1. The app embeds two trusted Ed25519 public keys: `current` and `next`. Either must verify the signature.
2. The signature is over the **canonicalized JSON** (sorted keys, no insignificant whitespace) of the bundle body.
3. The bundle's `valid_after` field must be ≥ the most recently installed bundle's `valid_after`. Older bundles are rejected (replay defense).
4. The bundle's `valid_after` must be ≤ the device's current wall-clock time + 5 minutes (clock-skew tolerance).
5. If verification fails for any reason, the previously installed bundle stays active. The app emits a metric, never silently downgrades.

## Top-level shape

```json
{
  "schema_version": 1,
  "bundle_version": 1234,
  "valid_after": "2026-05-10T00:00:00Z",
  "valid_until": "2026-08-10T00:00:00Z",
  "ssrs_registered_prefixes": [ ... ],
  "org_to_prefix_map": [ ... ],
  "blocked_urls": [ ... ],
  "blocked_domains": [ ... ],
  "blocked_senders": [ ... ],
  "allowlisted_senders": [ ... ],
  "rule_pack_version": 7
}
```

## `ssrs_registered_prefixes`

The full list of `#prefix` values currently registered with OFCA. Source: daily scrape of https://www.ofca.gov.hk/ssrs/ via `tools/ssrs-scraper/`. Format:

```json
{ "prefix": "hsbc", "registered_at": "2023-12-28", "category": "bank" }
```

Apps use this to answer "is this `#hsbc` SMS one the carrier already verified?". The mere presence of the prefix in the list means yes — the carrier blocks `#hsbc` from anyone other than the registered sender.

## `org_to_prefix_map`

The curated mapping from organization names (and aliases) to their expected `#prefix`. This drives the killer rule "body says HSBC but sender is not `#hsbc` → flag". Format:

```json
{
  "canonical_name": "HSBC",
  "aliases_zh_hk": ["匯豐", "匯豐銀行"],
  "aliases_en": ["HSBC", "Hongkong Shanghai Banking", "HSBC HK"],
  "expected_prefix": "hsbc",
  "category": "bank",
  "severity": "high"
}
```

This list is maintained manually based on the OFCA registry plus organization research. It must not be auto-generated — the cost of misclassifying a legitimate organization is too high.

## `blocked_urls`, `blocked_domains`, `blocked_senders`

Each entry is an opaque hash, not a plaintext value, **except** for the SSRS-derived data above (which is public information from OFCA). Format:

```json
{
  "hash": "9f86d081884c7d659a2feaa0c55ad015...",
  "kind": "url",
  "first_seen_at": "2026-04-15T12:00:00Z",
  "sources": ["scameter", "urlhaus"],
  "confidence": 0.95
}
```

The hash is SHA-256 over the canonicalized URL (lowercased scheme and host; path preserved; query stripped if it matches known tracker patterns). The same canonicalization is applied client-side before lookup.

`confidence` is in `[0, 1]` and drives whether the client treats this as a hard block (≥ 0.9), warning (≥ 0.6), or low-priority signal.

## `allowlisted_senders`

Senders that are **immune to crowdsourced blocking**. Source: OFCA SSRS registry + HKMA bank list + gov.hk domains + manually curated. Format:

```json
{
  "sender_pattern": "#hsbc",
  "kind": "exact_prefix",
  "source": "ofca_ssrs",
  "added_at": "2023-12-28"
}
```

The server consults this list before promoting any new entry to `blocked_senders`. See `docs/threat-model.md` § T3.

## Delta format

A delta bundle has the same top-level shape but each list contains only `added` and `removed` items:

```json
{
  "schema_version": 1,
  "from_version": 1233,
  "to_version": 1234,
  "valid_after": "2026-05-10T00:00:00Z",
  "blocked_urls": {
    "added": [ ... ],
    "removed": [ "9f86d081884c7d659a2feaa0c55ad015..." ]
  },
  ...
}
```

Apps apply deltas in version order. If the device has version 1230 and the latest is 1234, it fetches `bundle-delta-1230-1234.json` (the server stitches single-step deltas into multi-step deltas on demand). If a delta chain is missing for any reason, the app falls back to fetching the full bundle.

## Size budget

The full bundle target size is ≤ 2 MB compressed. At one million blocklist entries, each entry must be ≤ ~2 bytes after compression — achievable because hashes compress poorly but the JSON keys do compress and we expect tens of thousands of entries, not millions, in v1. If we exceed budget, we move to a binary format (CBOR + Bloom filter for membership) in v2.

## Build pipeline

`tools/blocklist-builder/` reads from these inputs:

- `tools/ssrs-scraper/output/registry.json` — daily output
- HKPF Scameter API responses — cached locally with TTL
- URLhaus feed — cached
- OpenPhish Community feed — cached
- `data/manual/org-to-prefix-map.json` — version-controlled, human-edited
- `data/manual/allowlist-extra.json` — version-controlled, human-edited

It produces a candidate bundle, runs the schema validator, signs it, and uploads to Cloudflare R2. The signing key is fetched only at sign time from an offline location — never committed, never in CI environment variables for general jobs.
