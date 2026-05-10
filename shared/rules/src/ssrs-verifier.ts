import type {
  IncomingSms,
  OrgPrefixMapping,
  SsrsRegistry,
  Verdict,
} from './types.js';

const HASH_PREFIX_PATTERN = /^#([A-Za-z0-9_]+)\b/;

export interface SsrsCheckOutcome {
  readonly outcome:
    | 'trusted_registered_prefix'
    | 'phishing_claims_org_without_prefix'
    | 'unknown_hash_prefix'
    | 'no_signal';
  readonly matchedOrg?: OrgPrefixMapping;
  readonly observedPrefix?: string;
}

export function checkSsrs(sms: IncomingSms, registry: SsrsRegistry): SsrsCheckOutcome {
  const observedPrefix = extractHashPrefix(sms.senderId);

  if (observedPrefix !== undefined) {
    const isRegistered = registry.registeredPrefixes.some(
      (p) => p.prefix.toLowerCase() === observedPrefix.toLowerCase(),
    );
    if (isRegistered) {
      return { outcome: 'trusted_registered_prefix', observedPrefix };
    }
    return { outcome: 'unknown_hash_prefix', observedPrefix };
  }

  const matchedOrg = findOrgClaimedInBody(sms.body, registry.orgToPrefix);
  if (matchedOrg !== undefined) {
    return { outcome: 'phishing_claims_org_without_prefix', matchedOrg };
  }

  return { outcome: 'no_signal' };
}

export function ssrsCheckToVerdict(outcome: SsrsCheckOutcome): Verdict | null {
  switch (outcome.outcome) {
    case 'trusted_registered_prefix':
      return {
        label: 'trusted',
        score: 1.0,
        firedRuleIds: ['ssrs.trusted_registered_prefix'],
        explanationKey: 'ssrs.trusted',
        explanationParams: { prefix: outcome.observedPrefix ?? '' },
      };
    case 'phishing_claims_org_without_prefix':
      return {
        label: 'high_confidence_phishing',
        score: 0.95,
        firedRuleIds: ['ssrs.org_claimed_without_prefix'],
        explanationKey: 'ssrs.org_claimed_without_prefix',
        explanationParams: {
          org: outcome.matchedOrg?.canonicalName ?? '',
          expected_prefix: outcome.matchedOrg?.expectedPrefix ?? '',
        },
      };
    case 'unknown_hash_prefix':
      return {
        label: 'suspicious',
        score: 0.4,
        firedRuleIds: ['ssrs.unknown_hash_prefix'],
        explanationKey: 'ssrs.unknown_hash_prefix',
        explanationParams: { prefix: outcome.observedPrefix ?? '' },
      };
    case 'no_signal':
      return null;
  }
}

function extractHashPrefix(senderId: string): string | undefined {
  const match = HASH_PREFIX_PATTERN.exec(senderId.trim());
  if (match === null || match[1] === undefined) {
    return undefined;
  }
  return match[1];
}

function findOrgClaimedInBody(
  body: string,
  orgs: readonly OrgPrefixMapping[],
): OrgPrefixMapping | undefined {
  const lowerBody = body.toLowerCase();
  for (const org of orgs) {
    for (const alias of org.aliasesZhHk) {
      if (body.includes(alias)) return org;
    }
    for (const alias of org.aliasesEn) {
      if (lowerBody.includes(alias.toLowerCase())) return org;
    }
  }
  return undefined;
}
