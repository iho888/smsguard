package hk.smsguard.app.rules

private val HASH_PREFIX_PATTERN = Regex("^#([A-Za-z0-9_]+)\\b")

sealed class SsrsCheckOutcome {
    data class TrustedRegisteredPrefix(val observedPrefix: String) : SsrsCheckOutcome()
    data class PhishingClaimsOrgWithoutPrefix(val matchedOrg: OrgPrefixMapping) : SsrsCheckOutcome()
    data class UnknownHashPrefix(val observedPrefix: String) : SsrsCheckOutcome()
    data object NoSignal : SsrsCheckOutcome()
}

fun checkSsrs(sms: IncomingSms, registry: SsrsRegistry): SsrsCheckOutcome {
    val observedPrefix = extractHashPrefix(sms.senderId)

    if (observedPrefix != null) {
        val isRegistered = registry.registeredPrefixes.any {
            it.prefix.equals(observedPrefix, ignoreCase = true)
        }
        return if (isRegistered) {
            SsrsCheckOutcome.TrustedRegisteredPrefix(observedPrefix)
        } else {
            SsrsCheckOutcome.UnknownHashPrefix(observedPrefix)
        }
    }

    val matchedOrg = findOrgClaimedInBody(sms.body, registry.orgToPrefix)
    return if (matchedOrg != null) {
        SsrsCheckOutcome.PhishingClaimsOrgWithoutPrefix(matchedOrg)
    } else {
        SsrsCheckOutcome.NoSignal
    }
}

fun ssrsCheckToVerdict(outcome: SsrsCheckOutcome): Verdict? = when (outcome) {
    is SsrsCheckOutcome.TrustedRegisteredPrefix -> Verdict(
        label = VerdictLabel.TRUSTED,
        score = 1.0,
        firedRuleIds = listOf("ssrs.trusted_registered_prefix"),
        explanationKey = "ssrs.trusted",
        explanationParams = mapOf("prefix" to outcome.observedPrefix),
    )
    // Detector handles this outcome contextually (URL presence boosts score).
    // Returning null here keeps SSRS from emitting a hard verdict on its own.
    is SsrsCheckOutcome.PhishingClaimsOrgWithoutPrefix -> null
    // OFCA SSRS gates the # prefix at the carrier, so an unknown-to-us
    // prefix is still a positive signal that a real org registered it —
    // not suspicion. Let content/blocklist rules drive the verdict.
    is SsrsCheckOutcome.UnknownHashPrefix -> null
    is SsrsCheckOutcome.NoSignal -> null
}

private fun extractHashPrefix(senderId: String): String? {
    val match = HASH_PREFIX_PATTERN.find(senderId.trim()) ?: return null
    return match.groupValues[1].ifEmpty { null }
}

private fun findOrgClaimedInBody(body: String, orgs: List<OrgPrefixMapping>): OrgPrefixMapping? {
    // Only consider an "org claim" when the org name appears at the start of
    // the message (within the first 30 chars). Real impersonation looks like
    // "[ORG]:" / "ORG: ...". Mid-text mentions ("防騙：警方提醒...", "Pay your
    // HSBC card at our shop") are not impersonation and shouldn't trigger.
    val head = body.take(30)
    val lowerHead = head.lowercase()
    for (org in orgs) {
        for (alias in org.aliasesZhHk) {
            if (head.contains(alias)) return org
        }
        for (alias in org.aliasesEn) {
            if (lowerHead.contains(alias.lowercase())) return org
        }
    }
    return null
}
