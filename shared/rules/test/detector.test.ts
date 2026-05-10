import { describe, expect, it } from 'vitest';
import type { BlocklistLookup, DetectorContext } from '../src/detector.js';
import { detect } from '../src/detector.js';
import { TEST_REGISTRY } from './fixtures/registry.js';
import { ALL_SAMPLES } from './fixtures/sample-sms.js';

const EMPTY_BLOCKLIST: BlocklistLookup = {
  isBlockedUrlCanonical: () => false,
  isBlockedDomain: () => false,
};

const CTX: DetectorContext = {
  registry: TEST_REGISTRY,
  blocklist: EMPTY_BLOCKLIST,
};

describe('detect', () => {
  for (const sample of ALL_SAMPLES) {
    it(`labels "${sample.name}" with at least the expected severity`, () => {
      const result = detect(sample.sms, CTX);
      const labelOrder = [
        'no_signal',
        'suspicious',
        'likely_scam',
        'high_confidence_phishing',
      ];
      const trustedExpected = sample.expectedTopLabel === 'trusted';
      if (trustedExpected) {
        expect(result.verdict.label).toBe('trusted');
        return;
      }
      const expectedIdx = labelOrder.indexOf(sample.expectedTopLabel);
      const actualIdx = labelOrder.indexOf(result.verdict.label);
      expect(actualIdx).toBeGreaterThanOrEqual(expectedIdx);
    });
  }

  it('uses blocklist hits to escalate verdict', () => {
    const ctx: DetectorContext = {
      registry: TEST_REGISTRY,
      blocklist: {
        isBlockedUrlCanonical: (c) => c === 'https://shady.example/x',
        isBlockedDomain: () => false,
      },
    };
    const result = detect(
      { senderId: '+852 9999 0000', body: 'Hi click https://shady.example/x' },
      ctx,
    );
    expect(['likely_scam', 'high_confidence_phishing']).toContain(
      result.verdict.label,
    );
    expect(result.verdict.firedRuleIds.some((id) => id.startsWith('blocklist.'))).toBe(
      true,
    );
  });

  it('returns no_signal for completely benign messages', () => {
    const result = detect(
      {
        senderId: '+852 2345 6789',
        body: 'Reminder: your dentist appointment is tomorrow at 10am.',
      },
      CTX,
    );
    expect(result.verdict.label).toBe('no_signal');
  });
});
