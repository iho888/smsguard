package hk.smsguard.app.rules

private data class ContentRule(
    val id: String,
    val category: ContentRuleCategory,
    val severity: Severity,
    val pattern: Regex,
)

private val CI = setOf(RegexOption.IGNORE_CASE)

private val RULES: List<ContentRule> = listOf(
    ContentRule(
        id = "urgency.account_will_be_frozen",
        category = ContentRuleCategory.URGENCY,
        severity = Severity.HIGH,
        pattern = Regex(
            "(帳戶將被凍結|户口将被冻结|account.{0,20}(will be|to be|about to be).{0,20}(frozen|suspended|locked|disabled))",
            CI,
        ),
    ),
    ContentRule(
        id = "urgency.act_within_hours",
        category = ContentRuleCategory.URGENCY,
        severity = Severity.MEDIUM,
        pattern = Regex(
            "(請於\\s*\\d+\\s*小時內|within\\s*\\d+\\s*hours?|expires?\\s*in\\s*\\d+\\s*(hours?|hrs?))",
            CI,
        ),
    ),
    ContentRule(
        id = "credential.verify_account",
        category = ContentRuleCategory.CREDENTIAL_REQUEST,
        severity = Severity.HIGH,
        pattern = Regex(
            "(驗證您?的?帳戶|验证您?的?账户|verify\\s+your\\s+(account|identity|details))",
            CI,
        ),
    ),
    ContentRule(
        id = "credential.update_personal_info",
        category = ContentRuleCategory.CREDENTIAL_REQUEST,
        severity = Severity.MEDIUM,
        pattern = Regex(
            "(更新您?的?(資料|個人資料|帳戶資料)|update\\s+your\\s+(personal\\s+)?(info|details|profile))",
            CI,
        ),
    ),
    ContentRule(
        id = "crypto.transfer_to_wallet",
        category = ContentRuleCategory.CRYPTO_PAYMENT,
        severity = Severity.HIGH,
        pattern = Regex(
            "(轉帳?到?(比特幣|BTC|USDT)|transfer.{0,20}(bitcoin|btc|usdt|crypto.{0,5}wallet))",
            CI,
        ),
    ),
    ContentRule(
        id = "fake_gov.immigration_dept",
        category = ContentRuleCategory.FAKE_GOVERNMENT,
        severity = Severity.LOW,
        pattern = Regex("(入境(事務)?處|香港入境處|immigration\\s+department)", CI),
    ),
    ContentRule(
        id = "fake_gov.police_force",
        category = ContentRuleCategory.FAKE_GOVERNMENT,
        severity = Severity.LOW,
        pattern = Regex("(警務處|香港警察|hong\\s+kong\\s+police\\s+force)", CI),
    ),
    ContentRule(
        id = "fake_gov.customs",
        category = ContentRuleCategory.FAKE_GOVERNMENT,
        severity = Severity.LOW,
        pattern = Regex("(海關|香港海關|hong\\s+kong\\s+customs)", CI),
    ),
    ContentRule(
        id = "fake_gov.hkid_blocked",
        category = ContentRuleCategory.FAKE_GOVERNMENT,
        severity = Severity.HIGH,
        pattern = Regex(
            "(您?的?(香港)?身份證(已)?(被)?(凍結|停用|註銷)|your\\s+hkid\\s+(has\\s+been|is)\\s+(blocked|suspended|frozen))",
            CI,
        ),
    ),
    ContentRule(
        id = "malicious.shortener_with_urgency",
        category = ContentRuleCategory.MALICIOUS_SHORT_URL,
        severity = Severity.MEDIUM,
        pattern = Regex(
            "(bit\\.ly|tinyurl\\.com|t\\.co|goo\\.gl|is\\.gd|cutt\\.ly|rebrand\\.ly|t\\.ly)/[A-Za-z0-9]+",
            CI,
        ),
    ),
    ContentRule(
        id = "malicious.suspicious_tld",
        category = ContentRuleCategory.MALICIOUS_SHORT_URL,
        severity = Severity.MEDIUM,
        pattern = Regex(
            "https?://[^\\s]+\\.(?:tk|top|xyz|click|country|gq|ml|cf|cn\\.com|icu)(?:/|\\b)",
            CI,
        ),
    ),
    ContentRule(
        id = "remit.stranger_account",
        category = ContentRuleCategory.REMIT_TO_STRANGER,
        severity = Severity.HIGH,
        pattern = Regex(
            "(請(立即)?匯款到|匯款至以下帳戶|please\\s+(remit|transfer)\\s+to\\s+(the\\s+following\\s+)?account)",
            CI,
        ),
    ),
    // Prize/lottery scam: claim-your-prize phrasing + a URL anywhere in the
    // body. The URL requirement distinguishes scam SMS from legit promo SMS
    // like "claim your reward at our store" (no URL, points to physical
    // channels).
    ContentRule(
        id = "prize_scam.claim_prize_with_url",
        category = ContentRuleCategory.PRIZE_SCAM,
        severity = Severity.HIGH,
        pattern = Regex(
            "(?=[\\s\\S]*(claim\\s+your\\s+(prize|price|reward|award|gift|winning|cash)|you\\s+(have|'?ve)\\s+won|congratulations.{0,30}(winner|prize|won)|您?已?中獎|恭喜.{0,10}中獎|領取.{0,15}(獎(品|金)|獎勵)))(?=[\\s\\S]*https?://)",
            CI,
        ),
    ),
)

fun findContentRuleHits(body: String): List<ContentRuleHit> {
    val hits = mutableListOf<ContentRuleHit>()
    for (rule in RULES) {
        if (rule.pattern.containsMatchIn(body)) {
            hits += ContentRuleHit(ruleId = rule.id, severity = rule.severity, category = rule.category)
        }
    }
    return hits
}

fun listAllRuleIds(): List<String> = RULES.map { it.id }
