import { findContentRuleHits } from './content-rules.js';
import { checkSsrs, ssrsCheckToVerdict } from './ssrs-verifier.js';
import type {
  ContentRuleHit,
  ExtractedUrl,
  IncomingSms,
  SsrsRegistry,
  Verdict,
  VerdictLabel,
} from './types.js';
import { extractUrls } from './url-extractor.js';

export interface BlocklistLookup {
  isBlockedUrlCanonical(canonical: string): boolean;
  isBlockedDomain(registrableDomain: string): boolean;
}

export interface DetectorContext {
  readonly registry: SsrsRegistry;
  readonly blocklist: BlocklistLookup;
}

export interface DetectionDetail {
  readonly verdict: Verdict;
  readonly extractedUrls: readonly ExtractedUrl[];
  readonly contentHits: readonly ContentRuleHit[];
}

export function detect(sms: IncomingSms, ctx: DetectorContext): DetectionDetail {
  const ssrsOutcome = checkSsrs(sms, ctx.registry);
  const ssrsVerdict = ssrsCheckToVerdict(ssrsOutcome);

  if (ssrsVerdict !== null && ssrsVerdict.label === 'trusted') {
    return { verdict: ssrsVerdict, extractedUrls: [], contentHits: [] };
  }

  // Carrier-verified #prefix that we don't recognize: trust the OFCA
  // registration. Content rules don't run for #prefix senders because the
  // whole SSRS scheme is "if you see #, the carrier already verified the
  // org behind it." Marketing copy from real registered orgs frequently
  // contains urgency/credential phrasing that would otherwise flag.
  if (ssrsOutcome.outcome === 'unknown_hash_prefix') {
    return {
      verdict: {
        label: 'no_signal',
        score: 0.0,
        firedRuleIds: [`ssrs.carrier_verified:${ssrsOutcome.observedPrefix ?? ''}`],
        explanationKey: 'ssrs.carrier_verified',
        explanationParams: { prefix: ssrsOutcome.observedPrefix ?? '' },
      },
      extractedUrls: [],
      contentHits: [],
    };
  }

  const urls = extractUrls(sms.body);
  const blockedHits: string[] = [];
  for (const u of urls) {
    if (ctx.blocklist.isBlockedUrlCanonical(u.canonical)) {
      blockedHits.push(`blocklist.url:${u.canonical}`);
    } else if (ctx.blocklist.isBlockedDomain(u.registrableDomain)) {
      blockedHits.push(`blocklist.domain:${u.registrableDomain}`);
    }
  }

  const contentHits = findContentRuleHits(sms.body);

  const fired: string[] = [];
  const signalScores: number[] = [];
  if (ssrsVerdict !== null) {
    fired.push(...ssrsVerdict.firedRuleIds);
    signalScores.push(ssrsVerdict.score);
  }
  // Org claim without prefix: a contributing signal whose weight depends on
  // whether the body also contains a URL. Plain mentions of HSBC/匯豐 in
  // marketing chatter shouldn't tip the verdict on their own; but org-name
  // at message start + a URL is a strong impersonation pattern.
  if (ssrsOutcome.outcome === 'phishing_claims_org_without_prefix') {
    if (urls.length > 0) {
      fired.push('ssrs.org_claimed_without_prefix_with_url');
      signalScores.push(0.85);
    } else {
      fired.push('ssrs.org_claimed_without_prefix');
      signalScores.push(0.3);
    }
  }
  for (const hit of blockedHits) {
    fired.push(hit);
    signalScores.push(0.9);
  }
  for (const hit of contentHits) {
    fired.push(`content.${hit.ruleId}`);
    signalScores.push(severityToScore(hit.severity));
  }

  const score = noisyOr(signalScores);

  const label = labelFromScore(score);
  const verdict: Verdict = {
    label,
    score,
    firedRuleIds: fired,
    explanationKey: pickExplanationKey(label, blockedHits, contentHits, ssrsVerdict, fired),
    explanationParams: {},
  };
  return { verdict, extractedUrls: urls, contentHits };
}

function severityToScore(s: 'high' | 'medium' | 'low'): number {
  if (s === 'high') return 0.7;
  if (s === 'medium') return 0.4;
  return 0.2;
}

function noisyOr(scores: readonly number[]): number {
  if (scores.length === 0) return 0;
  let prod = 1;
  for (const s of scores) {
    const clamped = Math.max(0, Math.min(1, s));
    prod *= 1 - clamped;
  }
  return 1 - prod;
}

function labelFromScore(score: number): VerdictLabel {
  if (score >= 0.9) return 'high_confidence_phishing';
  if (score >= 0.7) return 'likely_scam';
  if (score >= 0.4) return 'suspicious';
  return 'no_signal';
}

function pickExplanationKey(
  label: VerdictLabel,
  blockedHits: readonly string[],
  contentHits: readonly ContentRuleHit[],
  ssrsVerdict: Verdict | null,
  fired: readonly string[],
): string {
  if (ssrsVerdict !== null && ssrsVerdict.label !== 'no_signal') {
    return ssrsVerdict.explanationKey;
  }
  if (fired.some((f) => f.startsWith('ssrs.org_claimed_without_prefix'))) {
    return 'ssrs.org_claimed_without_prefix';
  }
  if (blockedHits.length > 0) return 'blocklist.url_or_domain';
  if (contentHits.length > 0) return `content.${contentHits[0]!.category}`;
  return `verdict.${label}`;
}
