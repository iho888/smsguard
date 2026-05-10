import type { IncomingSms } from '../../src/types.js';

export interface LabeledSms {
  readonly name: string;
  readonly sms: IncomingSms;
  readonly expectedTopLabel:
    | 'trusted'
    | 'high_confidence_phishing'
    | 'likely_scam'
    | 'suspicious'
    | 'no_signal';
}

export const TRUSTED_SAMPLES: readonly LabeledSms[] = [
  {
    name: 'hsbc_trusted_otp',
    sms: {
      senderId: '#hsbc',
      body: 'HSBC: Your one-time password is 481923. Do not share with anyone.',
    },
    expectedTopLabel: 'trusted',
  },
  {
    name: 'immd_trusted_appointment',
    sms: {
      senderId: '#immd',
      body: '入境事務處：您的預約已確認。請於指定日期攜帶身份證明文件前往辦理。',
    },
    expectedTopLabel: 'trusted',
  },
];

export const SCAM_SAMPLES: readonly LabeledSms[] = [
  {
    name: 'fake_hsbc_no_prefix_zh',
    sms: {
      senderId: '+852 6123 4567',
      body: '【匯豐銀行】您的帳戶將被凍結，請立即驗證您的帳戶: http://hsbc-verify.top/auth',
    },
    expectedTopLabel: 'high_confidence_phishing',
  },
  {
    name: 'fake_hsbc_no_prefix_en',
    sms: {
      senderId: '+1 555 010 1234',
      body: 'HSBC HK: Your account will be frozen. Verify your account here: https://bit.ly/3xyz123',
    },
    expectedTopLabel: 'high_confidence_phishing',
  },
  {
    name: 'fake_immigration_hkid',
    sms: {
      senderId: '+852 9876 5432',
      body: '香港入境處：您的香港身份證已被凍結。請於24小時內驗證您的帳戶。https://immd-verify.xyz/',
    },
    expectedTopLabel: 'high_confidence_phishing',
  },
  {
    name: 'crypto_remit',
    sms: {
      senderId: '+44 7700 900123',
      body: 'You owe HKD 10,000. Please transfer to bitcoin wallet within 24 hours or face arrest.',
    },
    expectedTopLabel: 'likely_scam',
  },
  {
    name: 'urgency_with_shortener',
    sms: {
      senderId: '+852 6555 1234',
      body: 'Your account expires in 6 hours. Update your details now: https://tinyurl.com/abc123',
    },
    expectedTopLabel: 'likely_scam',
  },
  {
    name: 'fake_wsd_with_url',
    sms: {
      senderId: '+852 9123 4567',
      body: '水務署: 您有未繳水費，請於3日內到 https://wsd-pay.com 繳交，否則停水',
    },
    expectedTopLabel: 'likely_scam',
  },
  {
    name: 'prize_claim_with_url_en',
    sms: {
      senderId: '+852 9876 5432',
      body: 'please visit http://abcxyz.com to claim your price',
    },
    expectedTopLabel: 'likely_scam',
  },
  {
    name: 'prize_claim_with_url_zh',
    sms: {
      senderId: '+852 5500 1234',
      body: '恭喜！您已中獎港幣10000元，請於 https://lucky-prize.com 領取獎金',
    },
    expectedTopLabel: 'likely_scam',
  },
];

export const BENIGN_SAMPLES: readonly LabeledSms[] = [
  {
    name: 'restaurant_reminder',
    sms: {
      senderId: '+852 2345 6789',
      body: 'Hi, this is a reminder for your reservation at 7pm tonight. See you soon!',
    },
    expectedTopLabel: 'no_signal',
  },
  {
    name: 'delivery_notification',
    sms: {
      senderId: '+852 3456 7890',
      body: 'Your parcel will arrive between 2pm and 5pm today. No action needed.',
    },
    expectedTopLabel: 'no_signal',
  },
  {
    name: 'unknown_hash_prefix_benign',
    sms: {
      senderId: '#csl',
      body: 'CSL: Your monthly bill of HKD 388.00 is now ready. Thank you.',
    },
    expectedTopLabel: 'no_signal',
  },
  {
    name: 'unknown_hash_prefix_marketing_with_urgency',
    sms: {
      senderId: '#cathay',
      body: 'Cathay: Your Asia Miles statement is ready. Verify your account at https://cathay.com/login. Offer expires in 24 hours.',
    },
    expectedTopLabel: 'no_signal',
  },
  {
    name: 'promo_mentions_bank_no_url',
    sms: {
      senderId: '+852 5500 0000',
      body: '10% off all main courses tonight for HSBC Visa cardholders. Quote code DINEHK at the till.',
    },
    expectedTopLabel: 'no_signal',
  },
  {
    name: 'mid_text_police_mention_benign',
    sms: {
      senderId: '+852 6789 0000',
      body: 'Anti-fraud reminder: Hong Kong Police remind citizens to beware of phishing SMS asking for OTPs.',
    },
    expectedTopLabel: 'no_signal',
  },
  {
    name: 'prize_claim_no_url_legit_promo',
    sms: {
      senderId: '+852 2200 0000',
      body: 'Claim your reward at our Mong Kok store this Saturday — show this SMS to staff.',
    },
    expectedTopLabel: 'no_signal',
  },
];

export const ALL_SAMPLES: readonly LabeledSms[] = [
  ...TRUSTED_SAMPLES,
  ...SCAM_SAMPLES,
  ...BENIGN_SAMPLES,
];
