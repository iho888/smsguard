import { describe, expect, it } from 'vitest';
import { findContentRuleHits, listAllRules } from '../src/content-rules.js';

describe('findContentRuleHits', () => {
  it('detects zh-HK 帳戶將被凍結 urgency phrase', () => {
    const hits = findContentRuleHits('您的帳戶將被凍結，請即驗證');
    expect(hits.some((h) => h.ruleId === 'urgency.account_will_be_frozen')).toBe(
      true,
    );
  });

  it('detects English account-frozen phrase', () => {
    const hits = findContentRuleHits(
      'Your account will be suspended unless you act now',
    );
    expect(hits.some((h) => h.ruleId === 'urgency.account_will_be_frozen')).toBe(
      true,
    );
  });

  it('detects credential.verify_account in English', () => {
    const hits = findContentRuleHits('Please verify your account immediately.');
    expect(
      hits.some((h) => h.ruleId === 'credential.verify_account'),
    ).toBe(true);
  });

  it('detects fake immigration department reference', () => {
    const hits = findContentRuleHits(
      '香港入境處通知：您的身份證已被凍結。',
    );
    expect(hits.some((h) => h.category === 'fake_government')).toBe(true);
  });

  it('detects bit.ly shortener', () => {
    const hits = findContentRuleHits('Click https://bit.ly/3abc123 to win!');
    expect(
      hits.some((h) => h.ruleId === 'malicious.shortener_with_urgency'),
    ).toBe(true);
  });

  it('detects suspicious .top TLD', () => {
    const hits = findContentRuleHits(
      'verify at http://hsbc-verify.top/auth now',
    );
    expect(hits.some((h) => h.ruleId === 'malicious.suspicious_tld')).toBe(true);
  });

  it('detects crypto transfer demand', () => {
    const hits = findContentRuleHits(
      'Transfer to bitcoin wallet bc1q... within 24h',
    );
    expect(
      hits.some((h) => h.ruleId === 'crypto.transfer_to_wallet'),
    ).toBe(true);
  });

  it('returns empty for benign message', () => {
    const hits = findContentRuleHits(
      'Your dinner reservation tonight at 7pm is confirmed.',
    );
    expect(hits).toHaveLength(0);
  });
});

describe('listAllRules', () => {
  it('returns at least 10 rules', () => {
    expect(listAllRules().length).toBeGreaterThanOrEqual(10);
  });

  it('every rule id is unique', () => {
    const ids = listAllRules().map((r) => r.id);
    expect(new Set(ids).size).toBe(ids.length);
  });
});
