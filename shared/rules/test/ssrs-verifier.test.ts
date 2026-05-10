import { describe, expect, it } from 'vitest';
import { checkSsrs, ssrsCheckToVerdict } from '../src/ssrs-verifier.js';
import { TEST_REGISTRY } from './fixtures/registry.js';

describe('checkSsrs', () => {
  it('returns trusted_registered_prefix for #hsbc when registered', () => {
    const result = checkSsrs(
      { senderId: '#hsbc', body: 'OTP 123456' },
      TEST_REGISTRY,
    );
    expect(result.outcome).toBe('trusted_registered_prefix');
    expect(result.observedPrefix).toBe('hsbc');
  });

  it('treats prefix matching case-insensitively', () => {
    const result = checkSsrs(
      { senderId: '#HSBC', body: 'OTP 123456' },
      TEST_REGISTRY,
    );
    expect(result.outcome).toBe('trusted_registered_prefix');
  });

  it('returns unknown_hash_prefix for # prefix not in registry', () => {
    const result = checkSsrs(
      { senderId: '#unknownbank', body: 'Hello' },
      TEST_REGISTRY,
    );
    expect(result.outcome).toBe('unknown_hash_prefix');
    expect(result.observedPrefix).toBe('unknownbank');
  });

  it('returns phishing_claims_org_without_prefix when body mentions HSBC but sender is not #hsbc', () => {
    const result = checkSsrs(
      {
        senderId: '+852 6123 4567',
        body: '【匯豐銀行】您的帳戶將被凍結，請點擊連結驗證',
      },
      TEST_REGISTRY,
    );
    expect(result.outcome).toBe('phishing_claims_org_without_prefix');
    expect(result.matchedOrg?.canonicalName).toBe('HSBC');
  });

  it('matches English alias HSBC HK in body', () => {
    const result = checkSsrs(
      {
        senderId: '+1 555 010 1234',
        body: 'HSBC HK: Your account will be frozen. Verify here.',
      },
      TEST_REGISTRY,
    );
    expect(result.outcome).toBe('phishing_claims_org_without_prefix');
    expect(result.matchedOrg?.canonicalName).toBe('HSBC');
  });

  it('returns no_signal for benign SMS with no org claim and no # prefix', () => {
    const result = checkSsrs(
      {
        senderId: '+852 9876 5432',
        body: 'Hi, this is a reminder for your appointment at 3pm.',
      },
      TEST_REGISTRY,
    );
    expect(result.outcome).toBe('no_signal');
  });

  it('does not treat mid-text mention of org name as a claim', () => {
    // Bare mention deep in a message body should not trigger org-claim — only
    // impersonation-style "[ORG]:" / "ORG: ..." openings count.
    const result = checkSsrs(
      {
        senderId: '+852 9876 5432',
        body: 'Anti-fraud reminder: Hong Kong Police remind citizens to beware of phishing SMS.',
      },
      TEST_REGISTRY,
    );
    expect(result.outcome).toBe('no_signal');
  });
});

describe('ssrsCheckToVerdict', () => {
  it('produces a trusted verdict with score 1.0', () => {
    const v = ssrsCheckToVerdict({
      outcome: 'trusted_registered_prefix',
      observedPrefix: 'hsbc',
    });
    expect(v).not.toBeNull();
    expect(v!.label).toBe('trusted');
    expect(v!.score).toBe(1.0);
  });

  it('returns null for phishing_claims_org_without_prefix (detector applies contextual signal)', () => {
    const v = ssrsCheckToVerdict({
      outcome: 'phishing_claims_org_without_prefix',
      matchedOrg: {
        canonicalName: 'HSBC',
        aliasesZhHk: [],
        aliasesEn: [],
        expectedPrefix: 'hsbc',
        category: 'bank',
        severity: 'high',
      },
    });
    expect(v).toBeNull();
  });

  it('returns null for no_signal outcome', () => {
    const v = ssrsCheckToVerdict({ outcome: 'no_signal' });
    expect(v).toBeNull();
  });

  it('returns null for unknown_hash_prefix (carrier already gated)', () => {
    const v = ssrsCheckToVerdict({
      outcome: 'unknown_hash_prefix',
      observedPrefix: 'csl',
    });
    expect(v).toBeNull();
  });
});
