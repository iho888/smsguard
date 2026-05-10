package hk.smsguard.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import hk.smsguard.app.data.FlagRecord
import hk.smsguard.app.data.FlagStore
import hk.smsguard.app.rules.BlocklistLookup
import hk.smsguard.app.rules.DEFAULT_REGISTRY
import hk.smsguard.app.rules.DetectorContext
import hk.smsguard.app.rules.IncomingSms
import hk.smsguard.app.rules.VerdictLabel
import hk.smsguard.app.rules.detect
import java.net.URI

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val grouped = messages.groupBy { it.originatingAddress ?: "unknown" }

        val ctx = DetectorContext(registry = DEFAULT_REGISTRY, blocklist = BlocklistLookup.EMPTY)

        for ((sender, parts) in grouped) {
            val body = parts.joinToString(separator = "") { it.messageBody ?: "" }
            if (body.isBlank()) continue

            val incoming = IncomingSms(senderId = sender, body = body)
            val detail = detect(incoming, ctx)

            if (detail.verdict.label == VerdictLabel.NO_SIGNAL || detail.verdict.label == VerdictLabel.TRUSTED) {
                continue
            }

            val record = FlagRecord(
                id = System.currentTimeMillis(),
                receivedAtMillis = System.currentTimeMillis(),
                senderId = sender,
                verdictLabel = detail.verdict.label,
                score = detail.verdict.score,
                firedRuleIds = detail.verdict.firedRuleIds,
                explanationKey = detail.verdict.explanationKey,
                firstUrlHostHint = detail.extractedUrls.firstOrNull()?.let { hostOf(it.canonical) },
            )
            FlagStore.add(record)
            SmsGuardNotifier.notifyFlag(context, record)
        }
    }

    private fun hostOf(canonicalUrl: String): String? = try {
        URI(canonicalUrl).host
    } catch (_: Exception) {
        null
    }
}
