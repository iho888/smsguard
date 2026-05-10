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
      // Detector handles this outcome contextually (URL presence boosts score).
      // Returning null here keeps SSRS from emitting a hard verdict on its own.
      return null;
    case 'unknown_hash_prefix':
      // OFCA SSRS gates the # prefix at the carrier, so an unknown-to-us
      // prefix is still a positive signal that a real org registered it —
      // not suspicion. Let content/blocklist rules drive the verdict.
      return null;
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
  // Only consider an "org claim" when the org name appears at the start of
  // the message (within the first 30 chars after stripping leading brackets/
  // whitespace). Real impersonation looks like "[ORG]:" / "ORG: ...". Mid-
  // text mentions ("防騙：警方提醒...", "Pay your HSBC card at our shop")
  // are not impersonation and shouldn't trigger this signal.
  const head = body.slice(0, 30);
  const lowerHead = head.toLowerCase();
  for (const org of orgs) {
    for (const alias of org.aliasesZhHk) {
      if (head.includes(alias)) return org;
    }
    for (const alias of org.aliasesEn) {
      if (lowerHead.includes(alias.toLowerCase())) return org;
    }
  }
  return undefined;
}
