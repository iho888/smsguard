# SMSGuard detector corpus

Labeled SMS samples used to measure detector precision and recall on every commit. The corpus runs in CI via `corpus.test.ts`.

## Format — JSONL (one JSON object per line)

```jsonl
{"name":"...","sender":"...","body":"...","label":"scam"|"benign","expectedTopLabel":"trusted"|"high_confidence_phishing"|"likely_scam"|"suspicious"|"no_signal","source":"..."}
```

Field meanings:

| field             | required | values                                                                                  |
| ----------------- | -------- | --------------------------------------------------------------------------------------- |
| `name`            | yes      | unique snake_case identifier — show up in test failure messages                          |
| `sender`          | yes      | exact `senderId` as it would appear on the device, including `#prefix` if applicable    |
| `body`            | yes      | full SMS body, UTF-8                                                                    |
| `label`           | yes      | ground truth — `scam` or `benign`. Drives precision/recall.                             |
| `expectedTopLabel`| yes      | the verdict label the detector *should* produce. Used for granular regression tests.   |
| `source`          | yes      | provenance — see below                                                                  |

## Sources

- `hand_written` — invented for testing rule logic
- `scameter` — pulled from HKPF Scameter public scam database
- `hkcert` — pulled from HKCERT fraud SMS samples
- `news_report` — extracted from a public HK news article (link in `notes` if useful)
- `user_inbox` — anonymized real inbox sample (PII redacted: names, account numbers, OTPs, full URLs replaced with hostname-only forms)

## Adding samples

1. Append a JSONL line to `seed.jsonl`. Keep the file sorted by `source` then `name` if you have many.
2. Run `npx vitest run test/corpus.test.ts` locally — confirm it parses and the metrics print.
3. Commit. CI will report new precision/recall numbers in its log.

## How precision and recall are computed

Each sample is "flagged" if the detector's verdict label is one of `suspicious`, `likely_scam`, or `high_confidence_phishing`. `trusted` and `no_signal` are "not flagged".

```
TP (true positive)  = label=scam   AND flagged
FP (false positive) = label=benign AND flagged
FN (false negative) = label=scam   AND NOT flagged
TN (true negative)  = label=benign AND NOT flagged

precision = TP / (TP + FP)   # of flagged messages, what fraction is real?
recall    = TP / (TP + FN)   # of real scams, what fraction did we catch?
F1        = 2 * P * R / (P + R)
```

## Privacy when using `user_inbox` samples

Before adding a real-inbox SMS to the corpus:
- Replace any phone number with `+852 9000 0000` style placeholder
- Replace any account number / HKID / OTP with `XXXX`
- Replace specific full URLs with the hostname only (so blocklists don't accidentally leak target URLs)
- Strip names — replace with role placeholders (`阿仔`, `daughter`, etc.)

Never commit real PII. The corpus ships in the repo and is therefore public.
