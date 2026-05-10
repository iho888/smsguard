import type { ContentRuleCategory, ContentRuleHit } from './types.js';

interface ContentRule {
  readonly id: string;
  readonly category: ContentRuleCategory;
  readonly severity: 'high' | 'medium' | 'low';
  readonly pattern: RegExp;
}

const RULES: readonly ContentRule[] = [
  {
    id: 'urgency.account_will_be_frozen',
    category: 'urgency',
    severity: 'high',
    pattern: /(帳戶將被凍結|户口将被冻结|account.{0,20}(will be|to be|about to be).{0,20}(frozen|suspended|locked|disabled))/i,
  },
  {
    id: 'urgency.act_within_hours',
    category: 'urgency',
    severity: 'medium',
    pattern: /(請於\s*\d+\s*小時內|within\s*\d+\s*hours?|expires?\s*in\s*\d+\s*(hours?|hrs?))/i,
  },
  {
    id: 'credential.verify_account',
    category: 'credential_request',
    severity: 'high',
    pattern: /(驗證您?的?帳戶|验证您?的?账户|verify\s+your\s+(account|identity|details))/i,
  },
  {
    id: 'credential.update_personal_info',
    category: 'credential_request',
    severity: 'medium',
    pattern: /(更新您?的?(資料|個人資料|帳戶資料)|update\s+your\s+(personal\s+)?(info|details|profile))/i,
  },
  {
    id: 'crypto.transfer_to_wallet',
    category: 'crypto_payment',
    severity: 'high',
    pattern: /(轉帳?到?(比特幣|BTC|USDT)|transfer.{0,20}(bitcoin|btc|usdt|crypto.{0,5}wallet))/i,
  },
  {
    id: 'fake_gov.immigration_dept',
    category: 'fake_government',
    severity: 'high',
    pattern: /(入境(事務)?處|香港入境處|immigration\s+department)/i,
  },
  {
    id: 'fake_gov.police_force',
    category: 'fake_government',
    severity: 'high',
    pattern: /(警務處|香港警察|hong\s+kong\s+police\s+force)/i,
  },
  {
    id: 'fake_gov.customs',
    category: 'fake_government',
    severity: 'medium',
    pattern: /(海關|香港海關|hong\s+kong\s+customs)/i,
  },
  {
    id: 'fake_gov.hkid_blocked',
    category: 'fake_government',
    severity: 'high',
    pattern: /(您?的?(香港)?身份證(已)?(被)?(凍結|停用|註銷)|your\s+hkid\s+(has\s+been|is)\s+(blocked|suspended|frozen))/i,
  },
  {
    id: 'malicious.shortener_with_urgency',
    category: 'malicious_short_url',
    severity: 'medium',
    pattern: /(bit\.ly|tinyurl\.com|t\.co|goo\.gl|is\.gd|cutt\.ly|rebrand\.ly|t\.ly)\/[A-Za-z0-9]+/i,
  },
  {
    id: 'malicious.suspicious_tld',
    category: 'malicious_short_url',
    severity: 'medium',
    pattern: /https?:\/\/[^\s]+\.(?:tk|top|xyz|click|country|gq|ml|cf|cn\.com|icu)(?:\/|\b)/i,
  },
  {
    id: 'remit.stranger_account',
    category: 'remit_to_stranger',
    severity: 'high',
    pattern: /(請(立即)?匯款到|匯款至以下帳戶|please\s+(remit|transfer)\s+to\s+(the\s+following\s+)?account)/i,
  },
];

export function findContentRuleHits(body: string): ContentRuleHit[] {
  const hits: ContentRuleHit[] = [];
  for (const rule of RULES) {
    if (rule.pattern.test(body)) {
      hits.push({
        ruleId: rule.id,
        severity: rule.severity,
        category: rule.category,
      });
    }
  }
  return hits;
}

export function listAllRules(): readonly ContentRule[] {
  return RULES;
}
