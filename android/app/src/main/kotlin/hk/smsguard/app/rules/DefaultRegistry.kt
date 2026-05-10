package hk.smsguard.app.rules

/**
 * Hardcoded SSRS registry covering the most common HK organizations targeted
 * by SMS scammers. Will be replaced by a live-fetched, signed bundle in
 * Phase 3. Keep this list small and high-precision — adding an org here
 * means we are willing to flag any SMS that mentions it but lacks the
 * matching #prefix.
 */
val DEFAULT_REGISTRY: SsrsRegistry = SsrsRegistry(
    registeredPrefixes = listOf(
        SsrsRegisteredPrefix("hsbc", "2023-12-28", OrgCategory.BANK),
        SsrsRegisteredPrefix("hangseng", "2023-12-28", OrgCategory.BANK),
        SsrsRegisteredPrefix("boc", "2023-12-28", OrgCategory.BANK),
        SsrsRegisteredPrefix("sccb", "2024-02-21", OrgCategory.BANK),
        SsrsRegisteredPrefix("citibank", "2024-02-21", OrgCategory.BANK),
        SsrsRegisteredPrefix("dbs", "2024-02-21", OrgCategory.BANK),
        SsrsRegisteredPrefix("immd", "2024-02-21", OrgCategory.GOV),
        SsrsRegisteredPrefix("hkpolice", "2024-02-21", OrgCategory.GOV),
        SsrsRegisteredPrefix("hkma", "2024-02-21", OrgCategory.GOV),
        SsrsRegisteredPrefix("customs", "2024-02-21", OrgCategory.GOV),
        SsrsRegisteredPrefix("ird", "2024-02-21", OrgCategory.GOV),
        SsrsRegisteredPrefix("dh", "2024-02-21", OrgCategory.GOV),
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
            canonicalName = "Hang Seng Bank",
            aliasesZhHk = listOf("恒生", "恒生銀行"),
            aliasesEn = listOf("Hang Seng", "Hang Seng Bank"),
            expectedPrefix = "hangseng",
            category = OrgCategory.BANK,
            severity = Severity.HIGH,
        ),
        OrgPrefixMapping(
            canonicalName = "Bank of China (HK)",
            aliasesZhHk = listOf("中銀香港", "中國銀行(香港)", "中銀"),
            aliasesEn = listOf("Bank of China", "BOC HK", "BOCHK"),
            expectedPrefix = "boc",
            category = OrgCategory.BANK,
            severity = Severity.HIGH,
        ),
        OrgPrefixMapping(
            canonicalName = "Standard Chartered HK",
            aliasesZhHk = listOf("渣打", "渣打銀行"),
            aliasesEn = listOf("Standard Chartered", "SCB HK"),
            expectedPrefix = "sccb",
            category = OrgCategory.BANK,
            severity = Severity.HIGH,
        ),
        OrgPrefixMapping(
            canonicalName = "Citibank HK",
            aliasesZhHk = listOf("花旗銀行", "花旗"),
            aliasesEn = listOf("Citibank", "Citi HK"),
            expectedPrefix = "citibank",
            category = OrgCategory.BANK,
            severity = Severity.HIGH,
        ),
        OrgPrefixMapping(
            canonicalName = "DBS HK",
            aliasesZhHk = listOf("星展", "星展銀行"),
            aliasesEn = listOf("DBS", "DBS Bank HK"),
            expectedPrefix = "dbs",
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
            aliasesZhHk = listOf("警務處", "香港警察", "警方"),
            aliasesEn = listOf("Hong Kong Police", "HK Police Force"),
            expectedPrefix = "hkpolice",
            category = OrgCategory.GOV,
            severity = Severity.HIGH,
        ),
        OrgPrefixMapping(
            canonicalName = "Hong Kong Monetary Authority",
            aliasesZhHk = listOf("金管局", "香港金管局"),
            aliasesEn = listOf("HKMA", "Hong Kong Monetary Authority"),
            expectedPrefix = "hkma",
            category = OrgCategory.GOV,
            severity = Severity.HIGH,
        ),
        OrgPrefixMapping(
            canonicalName = "HK Customs and Excise",
            aliasesZhHk = listOf("香港海關", "海關"),
            aliasesEn = listOf("HK Customs", "Customs and Excise"),
            expectedPrefix = "customs",
            category = OrgCategory.GOV,
            severity = Severity.HIGH,
        ),
        OrgPrefixMapping(
            canonicalName = "Inland Revenue Department",
            aliasesZhHk = listOf("稅務局", "香港稅務局"),
            aliasesEn = listOf("Inland Revenue", "IRD HK"),
            expectedPrefix = "ird",
            category = OrgCategory.GOV,
            severity = Severity.HIGH,
        ),
    ),
)
