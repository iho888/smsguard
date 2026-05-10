/**
 * OFCA SSRS scraper — skeleton.
 *
 * DO NOT RUN against the live OFCA site until the ToS verification in README.md
 * is complete. This file exists to define the function shape and output schema.
 */

export interface ScrapedRegistryEntry {
  readonly prefix: string;
  readonly registeredOrganization: string;
  readonly category: 'bank' | 'gov' | 'telco' | 'utility' | 'commerce' | 'other';
  readonly registeredAt: string;
}

export interface ScrapedRegistry {
  readonly scrapedAt: string;
  readonly sourceUrl: string;
  readonly entries: readonly ScrapedRegistryEntry[];
}

export interface ScraperOptions {
  readonly fetchImpl: typeof fetch;
  readonly userAgent: string;
  readonly sourceUrl: string;
}

export async function scrapeOfcaRegistry(
  options: ScraperOptions,
): Promise<ScrapedRegistry> {
  void options;
  throw new Error(
    'scrapeOfcaRegistry is not implemented. See tools/ssrs-scraper/README.md ' +
      'for the ToS-verification gate that must be cleared before implementing.',
  );
}

export function diffRegistries(
  previous: ScrapedRegistry,
  next: ScrapedRegistry,
): {
  added: readonly ScrapedRegistryEntry[];
  removed: readonly ScrapedRegistryEntry[];
} {
  const prevPrefixes = new Set(previous.entries.map((e) => e.prefix));
  const nextPrefixes = new Set(next.entries.map((e) => e.prefix));
  return {
    added: next.entries.filter((e) => !prevPrefixes.has(e.prefix)),
    removed: previous.entries.filter((e) => !nextPrefixes.has(e.prefix)),
  };
}
