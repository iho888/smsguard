package hk.smsguard.app.data

import hk.smsguard.app.rules.VerdictLabel

data class FlagRecord(
    val id: Long,
    val receivedAtMillis: Long,
    val senderId: String,
    val verdictLabel: VerdictLabel,
    val score: Double,
    val firedRuleIds: List<String>,
    val explanationKey: String,
    val firstUrlHostHint: String?,
)
