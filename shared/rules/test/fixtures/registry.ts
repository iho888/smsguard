import type { SsrsRegistry } from '../../src/types.js';

export const TEST_REGISTRY: SsrsRegistry = {
  registeredPrefixes: [
    { prefix: 'hsbc', registeredAt: '2023-12-28', category: 'bank' },
    { prefix: 'hangseng', registeredAt: '2023-12-28', category: 'bank' },
    { prefix: 'boc', registeredAt: '2023-12-28', category: 'bank' },
    { prefix: 'immd', registeredAt: '2024-02-21', category: 'gov' },
    { prefix: 'hkpolice', registeredAt: '2024-02-21', category: 'gov' },
    { prefix: 'hkma', registeredAt: '2024-02-21', category: 'gov' },
    { prefix: 'sccb', registeredAt: '2024-02-21', category: 'bank' },
  ],
  orgToPrefix: [
    {
      canonicalName: 'HSBC',
      aliasesZhHk: ['匯豐', '匯豐銀行'],
      aliasesEn: ['HSBC', 'Hongkong Shanghai Banking', 'HSBC HK'],
      expectedPrefix: 'hsbc',
      category: 'bank',
      severity: 'high',
    },
    {
      canonicalName: 'Hang Seng Bank',
      aliasesZhHk: ['恒生', '恒生銀行'],
      aliasesEn: ['Hang Seng', 'Hang Seng Bank'],
      expectedPrefix: 'hangseng',
      category: 'bank',
      severity: 'high',
    },
    {
      canonicalName: 'Bank of China (HK)',
      aliasesZhHk: ['中銀香港', '中國銀行(香港)', '中銀'],
      aliasesEn: ['Bank of China', 'BOC HK', 'BOCHK'],
      expectedPrefix: 'boc',
      category: 'bank',
      severity: 'high',
    },
    {
      canonicalName: 'Immigration Department',
      aliasesZhHk: ['入境事務處', '入境處', '香港入境處'],
      aliasesEn: ['Immigration Department', 'HK Immigration'],
      expectedPrefix: 'immd',
      category: 'gov',
      severity: 'high',
    },
    {
      canonicalName: 'Hong Kong Police Force',
      aliasesZhHk: ['警務處', '香港警察', '警方'],
      aliasesEn: ['Hong Kong Police', 'HK Police Force'],
      expectedPrefix: 'hkpolice',
      category: 'gov',
      severity: 'high',
    },
  ],
};
