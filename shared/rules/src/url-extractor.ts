import type { ExtractedUrl } from './types.js';

const URL_PATTERN =
  /\b((?:https?:\/\/|www\.)[^\s<>"'()]+|(?:[a-z0-9-]+\.)+[a-z]{2,}(?:\/[^\s<>"'()]*)?)/gi;

const TRACKING_QUERY_KEYS: ReadonlySet<string> = new Set([
  'utm_source',
  'utm_medium',
  'utm_campaign',
  'utm_term',
  'utm_content',
  'gclid',
  'fbclid',
  'mc_cid',
  'mc_eid',
  '_ga',
]);

const KNOWN_MULTIPART_TLDS: ReadonlySet<string> = new Set([
  'co.uk',
  'co.jp',
  'co.kr',
  'com.hk',
  'org.hk',
  'gov.hk',
  'edu.hk',
  'net.hk',
  'idv.hk',
  'com.cn',
  'com.tw',
  'com.au',
  'com.sg',
]);

export function extractUrls(body: string): ExtractedUrl[] {
  const matches = body.match(URL_PATTERN);
  if (matches === null) return [];

  const seen = new Set<string>();
  const out: ExtractedUrl[] = [];
  for (const raw of matches) {
    const canonical = canonicalizeUrl(raw);
    if (canonical === undefined) continue;
    if (seen.has(canonical)) continue;
    seen.add(canonical);
    const registrableDomain = registrableDomainOf(canonical);
    out.push({ raw, canonical, registrableDomain });
  }
  return out;
}

export function canonicalizeUrl(raw: string): string | undefined {
  let candidate = raw.trim();
  if (candidate.length === 0) return undefined;

  const explicitScheme = /^([a-z][a-z0-9+.-]*):/i.exec(candidate);
  if (explicitScheme !== null) {
    const scheme = explicitScheme[1]!.toLowerCase();
    if (scheme !== 'http' && scheme !== 'https') return undefined;
  } else {
    candidate = `http://${candidate}`;
  }

  let url: URL;
  try {
    url = new URL(candidate);
  } catch {
    return undefined;
  }

  if (url.protocol !== 'http:' && url.protocol !== 'https:') return undefined;

  url.protocol = url.protocol.toLowerCase();
  url.hostname = url.hostname.toLowerCase();

  if (
    (url.protocol === 'http:' && url.port === '80') ||
    (url.protocol === 'https:' && url.port === '443')
  ) {
    url.port = '';
  }

  for (const key of [...url.searchParams.keys()]) {
    if (TRACKING_QUERY_KEYS.has(key.toLowerCase())) {
      url.searchParams.delete(key);
    }
  }

  url.hash = '';

  return url.toString();
}

export function registrableDomainOf(canonicalUrl: string): string {
  let host: string;
  try {
    host = new URL(canonicalUrl).hostname.toLowerCase();
  } catch {
    return '';
  }
  if (host.length === 0) return '';

  const labels = host.split('.');
  if (labels.length < 2) return host;

  const lastTwo = labels.slice(-2).join('.');
  if (labels.length >= 3 && KNOWN_MULTIPART_TLDS.has(lastTwo)) {
    return labels.slice(-3).join('.');
  }
  return lastTwo;
}
