import { readFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { detect, type BlocklistLookup, type DetectorContext } from '../src/detector.js';
import type { VerdictLabel } from '../src/types.js';
import { TEST_REGISTRY } from './fixtures/registry.js';

interface CorpusEntry {
  readonly name: string;
  readonly sender: string;
  readonly body: string;
  readonly label: 'scam' | 'benign';
  readonly expectedTopLabel: VerdictLabel;
  readonly source: string;
}

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

function loadCorpus(): CorpusEntry[] {
  const raw = readFileSync(join(__dirname, 'corpus', 'seed.jsonl'), 'utf-8');
  return raw
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
    .map((line, idx) => {
      try {
        return JSON.parse(line) as CorpusEntry;
      } catch (e) {
        throw new Error(`corpus/seed.jsonl line ${idx + 1} is not valid JSON: ${(e as Error).message}`);
      }
    });
}

const EMPTY_BLOCKLIST: BlocklistLookup = {
  isBlockedUrlCanonical: () => false,
  isBlockedDomain: () => false,
};

const CTX: DetectorContext = {
  registry: TEST_REGISTRY,
  blocklist: EMPTY_BLOCKLIST,
};

const FLAGGED_LABELS: ReadonlySet<VerdictLabel> = new Set([
  'suspicious',
  'likely_scam',
  'high_confidence_phishing',
]);

interface Metrics {
  tp: number;
  fp: number;
  fn: number;
  tn: number;
  precision: number;
  recall: number;
  f1: number;
  fpEntries: CorpusEntry[];
  fnEntries: CorpusEntry[];
}

function evaluate(entries: readonly CorpusEntry[]): Metrics {
  let tp = 0,
    fp = 0,
    fn = 0,
    tn = 0;
  const fpEntries: CorpusEntry[] = [];
  const fnEntries: CorpusEntry[] = [];

  for (const entry of entries) {
    const result = detect({ senderId: entry.sender, body: entry.body }, CTX);
    const flagged = FLAGGED_LABELS.has(result.verdict.label);
    if (entry.label === 'scam' && flagged) tp++;
    else if (entry.label === 'benign' && flagged) {
      fp++;
      fpEntries.push(entry);
    } else if (entry.label === 'scam' && !flagged) {
      fn++;
      fnEntries.push(entry);
    } else tn++;
  }

  const precision = tp + fp === 0 ? 0 : tp / (tp + fp);
  const recall = tp + fn === 0 ? 0 : tp / (tp + fn);
  const f1 = precision + recall === 0 ? 0 : (2 * precision * recall) / (precision + recall);
  return { tp, fp, fn, tn, precision, recall, f1, fpEntries, fnEntries };
}

describe('detector corpus', () => {
  const entries = loadCorpus();
  const metrics = evaluate(entries);

  // Print a summary that shows up in vitest output (and therefore in CI logs).
  // Using console.log directly rather than test names so it's always visible.
  it('reports corpus metrics', () => {
    const scams = entries.filter((e) => e.label === 'scam').length;
    const benigns = entries.filter((e) => e.label === 'benign').length;

    console.log('\n=== SMSGuard detector corpus metrics ===');
    console.log(`Corpus size:  ${entries.length} (${scams} scams, ${benigns} benigns)`);
    console.log(
      `Confusion:    TP=${metrics.tp}  FP=${metrics.fp}  FN=${metrics.fn}  TN=${metrics.tn}`,
    );
    console.log(`Precision:    ${(metrics.precision * 100).toFixed(1)}%   (of flagged, % that are real scams)`);
    console.log(`Recall:       ${(metrics.recall * 100).toFixed(1)}%   (of real scams, % we caught)`);
    console.log(`F1:           ${(metrics.f1 * 100).toFixed(1)}%`);
    if (metrics.fpEntries.length > 0) {
      console.log(`\nFalse positives (legit messages we flagged):`);
      for (const e of metrics.fpEntries) console.log(`  - ${e.name} [${e.source}]`);
    }
    if (metrics.fnEntries.length > 0) {
      console.log(`\nFalse negatives (scams we missed):`);
      for (const e of metrics.fnEntries) console.log(`  - ${e.name} [${e.source}]`);
    }
    console.log('=== end corpus metrics ===\n');

    expect(entries.length).toBeGreaterThan(0);
  });

  // Soft floor: if precision drops below 0.85 or recall drops below 0.7, the
  // build fails. Tighten these as the corpus grows. Today the corpus is small
  // and hand-written, so these are easy to clear; they exist to catch bad
  // regressions, not to certify ship-readiness.
  it('precision is at least 0.85', () => {
    expect(metrics.precision).toBeGreaterThanOrEqual(0.85);
  });

  it('recall is at least 0.7', () => {
    expect(metrics.recall).toBeGreaterThanOrEqual(0.7);
  });

  // Granular regression: every entry should match its expectedTopLabel. This
  // is the same kind of check the existing detector.test.ts does on
  // ALL_SAMPLES — keep both for now; corpus is the path forward.
  for (const entry of entries) {
    it(`expects "${entry.name}" to land at ${entry.expectedTopLabel} or stricter`, () => {
      const result = detect({ senderId: entry.sender, body: entry.body }, CTX);
      const order: readonly VerdictLabel[] = [
        'no_signal',
        'suspicious',
        'likely_scam',
        'high_confidence_phishing',
      ];
      if (entry.expectedTopLabel === 'trusted') {
        expect(result.verdict.label).toBe('trusted');
        return;
      }
      const expectedIdx = order.indexOf(entry.expectedTopLabel);
      const actualIdx = order.indexOf(result.verdict.label);
      expect(actualIdx).toBeGreaterThanOrEqual(expectedIdx);
    });
  }
});
