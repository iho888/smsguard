package hk.smsguard.app.rules

data class IncomingSms(
    val senderId: String,
    val body: String,
)

enum class VerdictLabel {
    TRUSTED,
    HIGH_CONFIDENCE_PHISHING,
    LIKELY_SCAM,
    SUSPICIOUS,
    NO_SIGNAL,
}

data class Verdict(
    val label: VerdictLabel,
    val score: Double,
    val firedRuleIds: List<String>,
    val explanationKey: String,
    val explanationParams: Map<String, String>,
)

enum class OrgCategory { BANK, GOV, TELCO, UTILITY, COMMERCE, OTHER }

enum class Severity { HIGH, MEDIUM, LOW }

data class SsrsRegisteredPrefix(
    val prefix: String,
    val registeredAt: String,
    val category: OrgCategory,
)

data class OrgPrefixMapping(
    val canonicalName: String,
    val aliasesZhHk: List<String>,
    val aliasesEn: List<String>,
    val expectedPrefix: String,
    val category: OrgCategory,
    val severity: Severity,
)

data class SsrsRegistry(
    val registeredPrefixes: List<SsrsRegisteredPrefix>,
    val orgToPrefix: List<OrgPrefixMapping>,
)

data class ExtractedUrl(
    val raw: String,
    val canonical: String,
    val registrableDomain: String,
)

enum class ContentRuleCategory {
    URGENCY,
    CREDENTIAL_REQUEST,
    CRYPTO_PAYMENT,
    FAKE_GOVERNMENT,
    FAKE_BANK,
    MALICIOUS_SHORT_URL,
    REMIT_TO_STRANGER,
}

data class ContentRuleHit(
    val ruleId: String,
    val severity: Severity,
    val category: ContentRuleCategory,
)
