package hk.smsguard.app.rules

val TEST_REGISTRY: SsrsRegistry = SsrsRegistry(
    registeredPrefixes = listOf(
        SsrsRegisteredPrefix("hsbc", "2023-12-28", OrgCategory.BANK),
        SsrsRegisteredPrefix("hangseng", "2023-12-28", OrgCategory.BANK),
        SsrsRegisteredPrefix("boc", "2023-12-28", OrgCategory.BANK),
        SsrsRegisteredPrefix("immd", "2024-02-21", OrgCategory.GOV),
        SsrsRegisteredPrefix("hkpolice", "2024-02-21", OrgCategory.GOV),
        SsrsRegisteredPrefix("hkma", "2024-02-21", OrgCategory.GOV),
    ),
    orgToPrefix = listOf(
        OrgPrefixMapping(
            canonicalName = "HSBC",
            aliasesZhHk = listOf("匯豐", "匯豐銀行"),
            aliasesEn = listOf("HSBC", "Hongkong Shanghai Banking", "HSBC HK"),
            expectedPrefix = "hsbc",
            category = OrgCategory.BANK,
            severity = Severity.HIGH,
        ),
        OrgPrefixMapping(
            canonicalName = "Immigration Department",
            aliasesZhHk = listOf("入境事務處", "入境處", "香港入境處"),
            aliasesEn = listOf("Immigration Department", "HK Immigration"),
            expectedPrefix = "immd",
            category = OrgCategory.GOV,
            severity = Severity.HIGH,
        ),
        OrgPrefixMapping(
            canonicalName = "Hong Kong Police Force",
            aliasesZhHk = listOf("警務處", "香港警察"),
            aliasesEn = listOf("Hong Kong Police", "HK Police Force"),
            expectedPrefix = "hkpolice",
            category = OrgCategory.GOV,
            severity = Severity.HIGH,
        ),
        OrgPrefixMapping(
            canonicalName = "Water Supplies Department",
            aliasesZhHk = listOf("水務署", "香港水務署"),
            aliasesEn = listOf("Water Supplies Department", "WSD HK"),
            expectedPrefix = "wsd",
            category = OrgCategory.GOV,
            severity = Severity.HIGH,
        ),
    ),
)
