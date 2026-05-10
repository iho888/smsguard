package hk.smsguard.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import hk.smsguard.app.data.FlagRecord

/**
 * The "viral loop": when SMSGuard catches a scam, the user gets one tap to
 * warn family on WhatsApp / Signal / SMS / wherever. Each blocked scam is a
 * potential acquisition for SMSGuard via the friend who sees the warning.
 *
 * The share message must work in HK family group chats — that means:
 * - zh-HK first, English second (most HK families chat in mixed Cantonese/EN)
 * - punchy enough to read on a notification preview
 * - includes enough detail (sender, host, scam type) that the family can
 *   recognize the pattern if it hits them too
 * - install link prominent at the end
 */
object ShareActions {
    // TODO: replace with smsguard.hk or Play Store URL once we have one.
    const val INSTALL_URL = "https://github.com/iho888/smsguard"

    // HKPF Scameter — the public anti-fraud lookup service. Opens in browser
    // for the user to manually paste sender/URL for an official check.
    const val SCAMETER_URL = "https://www.scameter.gov.hk/"

    fun launchShare(context: Context, record: FlagRecord) {
        val text = buildShareText(record)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "詐騙警示 / Scam alert")
        }
        val chooser = Intent.createChooser(send, "警告家人 / Warn family").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun openScameter(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SCAMETER_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // No browser available. Silent — the button still gives feedback via
            // the chooser flow; the alternative is a toast which we'll add when
            // we have a localized string resource.
        }
    }

    fun buildShareText(record: FlagRecord): String {
        val sender = record.senderId
        val host = record.firstUrlHostHint ?: "(no link)"
        val (zh, en) = scamPatternDescription(record.firedRuleIds)
        return buildString {
            append("⚠️ 詐騙警示 / Scam alert\n\n")
            append("啱啱收到一條詐騙 SMS：\n")
            append("• 寄件人 / From: ").append(sender).append('\n')
            append("• 連結 / Link: ").append(host).append('\n')
            append("• 類型 / Type: ").append(zh).append(" / ").append(en).append("\n\n")
            append("千祈唔好 click 任何連結，唔好回覆，唔好打電話。\n")
            append("Don't tap any links, don't reply, don't call back.\n\n")
            append("SMSGuard 為我攔截咗呢條短訊。\n")
            append("免費下載 / Free download:\n")
            append(INSTALL_URL)
        }
    }

    private data class PatternLabel(val zh: String, val en: String)

    private fun scamPatternDescription(firedRuleIds: List<String>): PatternLabel {
        // Pick the most specific category present, in priority order.
        for (id in firedRuleIds) {
            when {
                id.startsWith("ssrs.org_claimed") ->
                    return PatternLabel("假冒香港機構", "Org impersonation")
                id.startsWith("content.fake_gov.hkid_blocked") ->
                    return PatternLabel("假冒政府凍結身份證", "Fake gov / HKID frozen")
                id.startsWith("content.fake_gov") ->
                    return PatternLabel("假冒政府部門", "Fake government")
                id.startsWith("content.prize_scam") ->
                    return PatternLabel("假冒中獎/獎金", "Fake prize / lottery")
                id.startsWith("content.crypto") ->
                    return PatternLabel("加密貨幣詐騙", "Crypto scam")
                id.startsWith("content.credential") ->
                    return PatternLabel("騙取帳戶資料", "Credential phishing")
                id.startsWith("content.remit") ->
                    return PatternLabel("要求匯款", "Remittance demand")
                id.startsWith("blocklist") ->
                    return PatternLabel("已知詐騙連結", "Known scam URL")
                id.startsWith("content.malicious") ->
                    return PatternLabel("可疑連結", "Suspicious URL")
                id.startsWith("content.urgency") ->
                    return PatternLabel("製造緊急感", "Urgency tactic")
            }
        }
        return PatternLabel("可疑模式", "Suspicious pattern")
    }
}
