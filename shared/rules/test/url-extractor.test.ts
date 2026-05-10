import { describe, expect, it } from 'vitest';
import {
  canonicalizeUrl,
  extractUrls,
  registrableDomainOf,
} from '../src/url-extractor.js';

describe('canonicalizeUrl', () => {
  it('lowercases scheme and host', () => {
    expect(canonicalizeUrl('HTTPS://Example.COM/Path')).toBe(
      'https://example.com/Path',
    );
  });

  it('strips default ports', () => {
    expect(canonicalizeUrl('http://example.com:80/x')).toBe(
      'http://example.com/x',
    );
    expect(canonicalizeUrl('https://example.com:443/x')).toBe(
      'https://example.com/x',
    );
  });

  it('strips tracking query params but preserves others', () => {
    const out = canonicalizeUrl(
      'https://example.com/x?utm_source=a&id=42&fbclid=z',
    );
    expect(out).toBe('https://example.com/x?id=42');
  });

  it('strips fragment', () => {
    expect(canonicalizeUrl('https://example.com/x#section')).toBe(
      'https://example.com/x',
    );
  });

  it('adds http:// to a bare domain', () => {
    expect(canonicalizeUrl('www.example.com/x')).toBe(
      'http://www.example.com/x',
    );
  });

  it('returns undefined for non-http(s) schemes', () => {
    expect(canonicalizeUrl('ftp://example.com')).toBeUndefined();
  });
});

describe('registrableDomainOf', () => {
  it('returns eTLD+1 for plain TLDs', () => {
    expect(registrableDomainOf('https://www.example.com/x')).toBe(
      'example.com',
    );
  });

  it('returns eTLD+2 for known multi-part HK TLDs', () => {
    expect(registrableDomainOf('https://www.hsbc.com.hk/login')).toBe(
      'hsbc.com.hk',
    );
    expect(registrableDomainOf('https://www.gov.hk/abc')).toBe('www.gov.hk');
  });

  it('handles bare two-label hosts', () => {
    expect(registrableDomainOf('https://example.com')).toBe('example.com');
  });
});

describe('extractUrls', () => {
  it('extracts a single URL with full scheme', () => {
    const urls = extractUrls('Click here: https://bit.ly/3xyz123 to verify.');
    expect(urls).toHaveLength(1);
    expect(urls[0]!.canonical).toBe('https://bit.ly/3xyz123');
    expect(urls[0]!.registrableDomain).toBe('bit.ly');
  });

  it('extracts a www-prefix URL', () => {
    const urls = extractUrls('Visit www.example.com for details.');
    expect(urls).toHaveLength(1);
    expect(urls[0]!.canonical).toBe('http://www.example.com/');
  });

  it('deduplicates URLs by canonical form', () => {
    const urls = extractUrls(
      'Go https://Example.com/x and also HTTPS://example.COM/x?utm_source=a',
    );
    expect(urls).toHaveLength(1);
  });

  it('returns empty list when no URL is present', () => {
    expect(extractUrls('Hello, no link here.')).toHaveLength(0);
  });

  it('extracts URL with .top suspicious TLD', () => {
    const urls = extractUrls(
      'Verify: http://hsbc-verify.top/auth right now',
    );
    expect(urls).toHaveLength(1);
    expect(urls[0]!.registrableDomain).toBe('hsbc-verify.top');
  });
});
