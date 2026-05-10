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
  if (ssrsVerdict !== null && ssrsVerdict.label === 'high_confidence_phishing') {
    return { verdict: ssrsVerdict, extractedUrls: extractUrls(sms.body), contentHits: findContentRuleHits(sms.body) };
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
    explanationKey: pickExplanationKey(label, blockedHits, contentHits, ssrsVerdict),
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
): string {
  if (ssrsVerdict !== null && ssrsVerdict.label !== 'no_signal') {
    return ssrsVerdict.explanationKey;
  }
  if (blockedHits.length > 0) return 'blocklist.url_or_domain';
  if (contentHits.length > 0) return `content.${contentHits[0]!.category}`;
  return `verdict.${label}`;
}
