export type Locale = 'zh-HK' | 'en';

export interface IncomingSms {
  readonly senderId: string;
  readonly body: string;
}

export type VerdictLabel =
  | 'trusted'
  | 'high_confidence_phishing'
  | 'likely_scam'
  | 'suspicious'
  | 'no_signal';

export interface Verdict {
  readonly label: VerdictLabel;
  readonly score: number;
  readonly firedRuleIds: readonly string[];
  readonly explanationKey: string;
  readonly explanationParams: Readonly<Record<string, string>>;
}

export interface SsrsRegisteredPrefix {
  readonly prefix: string;
  readonly registeredAt: string;
  readonly category: 'bank' | 'gov' | 'telco' | 'utility' | 'commerce' | 'other';
}

export interface OrgPrefixMapping {
  readonly canonicalName: string;
  readonly aliasesZhHk: readonly string[];
  readonly aliasesEn: readonly string[];
  readonly expectedPrefix: string;
  readonly category: 'bank' | 'gov' | 'telco' | 'utility' | 'commerce' | 'other';
  readonly severity: 'high' | 'medium' | 'low';
}

export interface SsrsRegistry {
  readonly registeredPrefixes: readonly SsrsRegisteredPrefix[];
  readonly orgToPrefix: readonly OrgPrefixMapping[];
}

export interface ExtractedUrl {
  readonly raw: string;
  readonly canonical: string;
  readonly registrableDomain: string;
}

export interface ContentRuleHit {
  readonly ruleId: string;
  readonly severity: 'high' | 'medium' | 'low';
  readonly category: ContentRuleCategory;
}

export type ContentRuleCategory =
  | 'urgency'
  | 'credential_request'
  | 'crypto_payment'
  | 'fake_government'
  | 'fake_bank'
  | 'malicious_short_url'
  | 'remit_to_stranger'
  | 'prize_scam';
