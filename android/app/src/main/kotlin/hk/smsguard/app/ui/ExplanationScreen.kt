package hk.smsguard.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hk.smsguard.app.data.FlagRecord
import hk.smsguard.app.data.SenderBlocklist
import hk.smsguard.app.rules.VerdictLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplanationScreen(record: FlagRecord?, onClose: () -> Unit) {
    val context = LocalContext.current
    var showBlockConfirm by remember { mutableStateOf(false) }
    var blockedThisSession by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMSGuard") },
                navigationIcon = { TextButton(onClick = onClose) { Text("✕") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (record == null) {
                Spacer(Modifier.height(8.dp))
                Text("This alert is no longer available.")
                Button(onClick = onClose) { Text("Close") }
                return@Column
            }

            Spacer(Modifier.height(4.dp))

            // 1. Calm but unmistakable verdict — bilingual, biggest text on screen.
            VerdictHeadlineCard(record)

            // 2. What to do, right now. Three short imperative lines so a panicked
            //    user sees actions before they read the technical explanation.
            DontDoThisCard()

            // 3. Three primary actions stacked, in viral-priority order.
            Button(
                onClick = { ShareActions.launchShare(context, record) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) { Text("⚠️ 警告家人 / Warn my family") }

            FilledTonalButton(
                onClick = { ShareActions.openScameter(context) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("📤 報告 Scameter / Report to Scameter") }

            OutlinedButton(
                onClick = { showBlockConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !blockedThisSession,
            ) {
                Text(
                    if (blockedThisSession) "✓ 已拉黑 / Sender blocked"
                    else "🚫 拉黑寄件人 / Block sender",
                )
            }

            // 4. Details — sender, linked host, fired rules. Below the fold by
            //    design: panicked users don't need to read regex rule names.
            Spacer(Modifier.height(4.dp))
            DetailsCard(record)
        }
    }

    if (showBlockConfirm && record != null) {
        BlockSenderDialog(
            sender = record.senderId,
            onConfirm = {
                SenderBlocklist.block(record.senderId)
                blockedThisSession = true
                showBlockConfirm = false
            },
            onDismiss = { showBlockConfirm = false },
        )
    }
}

@Composable
private fun VerdictHeadlineCard(record: FlagRecord) {
    val (zh, en) = headlineFor(record.verdictLabel)
    val container = when (record.verdictLabel) {
        VerdictLabel.HIGH_CONFIDENCE_PHISHING,
        VerdictLabel.LIKELY_SCAM -> MaterialTheme.colorScheme.errorContainer
        VerdictLabel.SUSPICIOUS -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val onContainer = when (record.verdictLabel) {
        VerdictLabel.HIGH_CONFIDENCE_PHISHING,
        VerdictLabel.LIKELY_SCAM -> MaterialTheme.colorScheme.onErrorContainer
        VerdictLabel.SUSPICIOUS -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "⚠️ $zh",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                en,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DontDoThisCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "千祈唔好 / Right now, do NOT:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text("❶ 唔好 click 任何連結 — Don't tap any links")
            Text("❷ 唔好回覆短訊 — Don't reply")
            Text("❸ 唔好打電話過去 — Don't call back")
            Text("❹ 唔好俾任何個人資料 — Don't give any personal info")
            Spacer(Modifier.height(8.dp))
            Text(
                "如果懷疑被騙，立即報警 999。\nIf you think you were scammed, call 999 immediately.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DetailsCard(record: FlagRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("詳情 / Details", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            Text("寄件人 / Sender", style = MaterialTheme.typography.labelMedium)
            Text(record.senderId, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))

            if (record.firstUrlHostHint != null) {
                Text("連結指向 / Linked host", style = MaterialTheme.typography.labelMedium)
                Text(record.firstUrlHostHint, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }

            Text("為何可疑 / Why this is suspicious", style = MaterialTheme.typography.labelMedium)
            Text(explanationBody(record), style = MaterialTheme.typography.bodyMedium)

            if (record.firedRuleIds.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("觸發規則 / Rules fired", style = MaterialTheme.typography.labelMedium)
                for (id in record.firedRuleIds) {
                    Text("• $id", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun BlockSenderDialog(
    sender: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("拉黑寄件人 / Block sender?") },
        text = {
            Text(
                "之後 SMSGuard 唔會再為由 \"$sender\" 寄出嘅短訊出通知。" +
                    "你嘅短訊 app 仍然會正常顯示。\n\n" +
                    "Future SMS from \"$sender\" will not raise SMSGuard alerts. " +
                    "Your normal Messages app will still receive them.",
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("拉黑 / Block") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消 / Cancel") } },
    )
}

private data class HeadlinePair(val zh: String, val en: String)

private fun headlineFor(label: VerdictLabel): HeadlinePair = when (label) {
    VerdictLabel.HIGH_CONFIDENCE_PHISHING -> HeadlinePair("呢條短訊極大機會係詐騙", "This is almost certainly a scam")
    VerdictLabel.LIKELY_SCAM -> HeadlinePair("呢條短訊好可能係詐騙", "This SMS is probably a scam")
    VerdictLabel.SUSPICIOUS -> HeadlinePair("呢條短訊有啲可疑", "This SMS is suspicious")
    VerdictLabel.TRUSTED -> HeadlinePair("呢條短訊係可信嘅", "Looks trustworthy")
    VerdictLabel.NO_SIGNAL -> HeadlinePair("無發現可疑特徵", "No signal detected")
}

private fun explanationBody(record: FlagRecord): String {
    return when (record.explanationKey) {
        "ssrs.org_claimed_without_prefix" ->
            "呢條短訊聲稱嚟自一間香港機構，但寄件人並無使用 OFCA 註冊嘅 #prefix。" +
                "真實嘅銀行同政府部門會用經過電訊商驗證嘅 #prefix。\n\n" +
                "This message claims to be from a HK organization but the sender ID does not start with the OFCA-registered #prefix. Real banks/govt departments use #prefixes that the carrier verifies."
        "ssrs.unknown_hash_prefix" ->
            "寄件人睇落似 #prefix，但唔喺 OFCA SMS Sender Registration 名單上。\n\n" +
                "The sender ID looks like a #prefix but isn't on the OFCA registration list."
        "ssrs.carrier_verified" ->
            "呢個 #prefix 已經被電訊商驗證，係 OFCA 註冊嘅機構發出。\n\n" +
                "This #prefix was carrier-verified under the OFCA SMS Sender Registration Scheme — the org behind it is registered, even if SMSGuard doesn't recognize this specific prefix."
        "blocklist.url_or_domain" ->
            "短訊入面嘅連結已經喺 SMSGuard 嘅黑名單上。\n\n" +
                "The link in this message matches an entry on the SMSGuard blocklist."
        else -> {
            val first = record.firedRuleIds.firstOrNull() ?: "unknown"
            "觸發規則：$first。短訊入面嘅用字或連結符合已知詐騙模式。\n\n" +
                "Rule fired: $first. The wording or links in this message match a known scam pattern."
        }
    }
}
