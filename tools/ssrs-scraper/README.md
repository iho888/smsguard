# SSRS Scraper

Daily fetch of the OFCA SMS Sender Registration Scheme (SSRS) public registry from https://www.ofca.gov.hk/ssrs/, normalized into the format consumed by `tools/blocklist-builder/`.

## Status

**Skeleton only.** Before this script is run against the live OFCA site:

1. **ToS verification** — confirm OFCA's terms permit programmatic fetching and redistribution. The registry is described as "public information", but redistribution rights need explicit confirmation. If unclear, contact OFCA. Document the response in `docs/data-sources.md` (TBD).
2. **Rate limit policy** — implement a polite fetch interval (e.g., once per 24 hours, single request). Identify ourselves with a descriptive User-Agent that includes a contact URL.
3. **Caching** — keep the most recent 30 days of scraped registry snapshots so we can detect anomalies (e.g., an entry suddenly disappearing).
4. **Diff alerting** — if more than X% of entries change between two consecutive scrapes, alert a human before pushing to the blocklist bundle build.

The scraper output schema is `tools/ssrs-scraper/schema.json` (TBD when the live page structure is inspected). The output file is `tools/ssrs-scraper/output/registry.json`.

## Output schema (target)

```json
{
  "scraped_at": "2026-05-10T08:30:00+08:00",
  "source_url": "https://www.ofca.gov.hk/ssrs/",
  "entries": [
    {
      "prefix": "hsbc",
      "registered_organization": "The Hongkong and Shanghai Banking Corporation Limited",
      "category": "bank",
      "registered_at": "2023-12-28"
    }
  ]
}
```

## Implementation plan

`scrape.ts` (skeleton in this directory) will:

1. Fetch the registry HTML page with a timeout and a single retry on 5xx.
2. Parse the table(s) listing registered senders. The page structure will guide the parser; defensive coding required (the page is human-edited).
3. Validate the parsed output against `schema.json`.
4. Compare against the previous snapshot and produce a diff log.
5. If validation passes and diff is within thresholds, write `output/registry.json`. Otherwise exit non-zero.

This script is **not yet runnable**. Implementing it requires inspecting the live OFCA page and confirming the data structure. Do not run against the live site without completing the ToS verification above.
