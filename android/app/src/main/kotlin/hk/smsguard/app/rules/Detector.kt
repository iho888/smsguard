package hk.smsguard.app.rules

interface BlocklistLookup {
    fun isBlockedUrlCanonical(canonical: String): Boolean
    fun isBlockedDomain(registrableDomain: String): Boolean

    companion object {
        val EMPTY: BlocklistLookup = object : BlocklistLookup {
            override fun isBlockedUrlCanonical(canonical: String) = false
            override fun isBlockedDomain(registrableDomain: String) = false
        }
    }
}

data class DetectorContext(
    val registry: SsrsRegistry,
    val blocklist: BlocklistLookup,
)

data class DetectionDetail(
    val verdict: Verdict,
    val extractedUrls: List<ExtractedUrl>,
    val contentHits: List<ContentRuleHit>,
)

fun detect(sms: IncomingSms, ctx: DetectorContext): DetectionDetail {
    val ssrsOutcome = checkSsrs(sms, ctx.registry)
    val ssrsVerdict = ssrsCheckToVerdict(ssrsOutcome)

    if (ssrsVerdict != null && ssrsVerdict.label == VerdictLabel.TRUSTED) {
        return DetectionDetail(verdict = ssrsVerdict, extractedUrls = emptyList(), contentHits = emptyList())
    }

    // Carrier-verified #prefix that we don't recognize: trust the OFCA
    // registration. Content rules don't run for #prefix senders because the
    // whole SSRS scheme is "if you see #, the carrier already verified the
    // org behind it." Marketing copy from real registered orgs frequently
    // contains urgency/credential phrasing that would otherwise flag.
    if (ssrsOutcome is SsrsCheckOutcome.UnknownHashPrefix) {
        return DetectionDetail(
            verdict = Verdict(
                label = VerdictLabel.NO_SIGNAL,
                score = 0.0,
                firedRuleIds = listOf("ssrs.carrier_verified:${ssrsOutcome.observedPrefix}"),
                explanationKey = "ssrs.carrier_verified",
                explanationParams = mapOf("prefix" to ssrsOutcome.observedPrefix),
            ),
            extractedUrls = emptyList(),
            contentHits = emptyList(),
        )
    }

    val urls = extractUrls(sms.body)
    val blockedHits = mutableListOf<String>()
    for (u in urls) {
        when {
            ctx.blocklist.isBlockedUrlCanonical(u.canonical) -> blockedHits += "blocklist.url:${u.canonical}"
            ctx.blocklist.isBlockedDomain(u.registrableDomain) -> blockedHits += "blocklist.domain:${u.registrableDomain}"
        }
    }

    val contentHits = findContentRuleHits(sms.body)

    val fired = mutableListOf<String>()
    val signalScores = mutableListOf<Double>()
    if (ssrsVerdict != null) {
        fired += ssrsVerdict.firedRuleIds
        signalScores += ssrsVerdict.score
    }
    // Org claim without prefix: contributing signal whose weight depends on
    // whether the body also has a URL. Plain bank/dept mentions in marketing
    // chatter shouldn't tip the verdict alone, but org-name at message start
    // + a URL is a strong impersonation pattern.
    if (ssrsOutcome is SsrsCheckOutcome.PhishingClaimsOrgWithoutPrefix) {
        if (urls.isNotEmpty()) {
            fired += "ssrs.org_claimed_without_prefix_with_url"
            signalScores += 0.85
        } else {
            fired += "ssrs.org_claimed_without_prefix"
            signalScores += 0.3
        }
    }
    for (hit in blockedHits) {
        fired += hit
        signalScores += 0.9
    }
    for (hit in contentHits) {
        fired += "content.${hit.ruleId}"
        signalScores += severityToScore(hit.severity)
    }

    val score = noisyOr(signalScores)
    val label = labelFromScore(score)
    val verdict = Verdict(
        label = label,
        score = score,
        firedRuleIds = fired,
        explanationKey = pickExplanationKey(label, blockedHits, contentHits, ssrsVerdict, fired),
        explanationParams = emptyMap(),
    )
    return DetectionDetail(verdict = verdict, extractedUrls = urls, contentHits = contentHits)
}

private fun severityToScore(s: Severity): Double = when (s) {
    Severity.HIGH -> 0.7
    Severity.MEDIUM -> 0.4
    Severity.LOW -> 0.2
}

private fun noisyOr(scores: List<Double>): Double {
    if (scores.isEmpty()) return 0.0
    var prod = 1.0
    for (s in scores) {
        val clamped = s.coerceIn(0.0, 1.0)
        prod *= 1.0 - clamped
    }
    return 1.0 - prod
}

private fun labelFromScore(score: Double): VerdictLabel = when {
    score >= 0.9 -> VerdictLabel.HIGH_CONFIDENCE_PHISHING
    score >= 0.7 -> VerdictLabel.LIKELY_SCAM
    score >= 0.4 -> VerdictLabel.SUSPICIOUS
    else -> VerdictLabel.NO_SIGNAL
}

private fun pickExplanationKey(
    label: VerdictLabel,
    blockedHits: List<String>,
    contentHits: List<ContentRuleHit>,
    ssrsVerdict: Verdict?,
    fired: List<String>,
): String {
    if (ssrsVerdict != null && ssrsVerdict.label != VerdictLabel.NO_SIGNAL) return ssrsVerdict.explanationKey
    if (fired.any { it.startsWith("ssrs.org_claimed_without_prefix") }) return "ssrs.org_claimed_without_prefix"
    if (blockedHits.isNotEmpty()) return "blocklist.url_or_domain"
    if (contentHits.isNotEmpty()) return "content.${contentHits[0].category.name.lowercase()}"
    return "verdict.${label.name.lowercase()}"
}
