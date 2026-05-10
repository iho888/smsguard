package hk.smsguard.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hk.smsguard.app.data.FlagRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplanationScreen(record: FlagRecord?, onClose: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Why was this flagged?") },
                navigationIcon = {
                    TextButton(onClick = onClose) { Text("✕") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (record == null) {
                Text("This alert is no longer available.")
                Button(onClick = onClose) { Text("Close") }
                return@Column
            }

            Text(
                explainHeadline(record),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sender", fontWeight = FontWeight.SemiBold)
                    Text(record.senderId, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    if (record.firstUrlHostHint != null) {
                        Text("Linked host", fontWeight = FontWeight.SemiBold)
                        Text(record.firstUrlHostHint, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                    }
                    Text("Why this is suspicious", fontWeight = FontWeight.SemiBold)
                    Text(explanationBody(record), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Rules that fired", fontWeight = FontWeight.SemiBold)
                    for (id in record.firedRuleIds) {
                        Text("• $id", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Reminder", fontWeight = FontWeight.SemiBold)
                    Text(
                        "If in doubt, do not click any links. Contact the organization directly using a number you found yourself, not from the SMS.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private fun explainHeadline(record: FlagRecord): String {
    return when (record.verdictLabel) {
        hk.smsguard.app.rules.VerdictLabel.HIGH_CONFIDENCE_PHISHING -> "This is almost certainly a scam"
        hk.smsguard.app.rules.VerdictLabel.LIKELY_SCAM -> "This SMS is probably a scam"
        hk.smsguard.app.rules.VerdictLabel.SUSPICIOUS -> "This SMS is suspicious"
        hk.smsguard.app.rules.VerdictLabel.TRUSTED -> "Looks trustworthy"
        hk.smsguard.app.rules.VerdictLabel.NO_SIGNAL -> "No signal detected"
    }
}

private fun explanationBody(record: FlagRecord): String {
    return when (record.explanationKey) {
        "ssrs.org_claimed_without_prefix" ->
            "The message claims to be from a Hong Kong organization that has registered with OFCA, but the sender ID does not start with the registered #prefix. Real banks and government departments use #prefixes that the carrier verifies before delivery."
        "ssrs.unknown_hash_prefix" ->
            "The sender ID looks like a #prefix but isn't on the OFCA SMS Sender Registration list."
        "ssrs.carrier_verified" ->
            "This sender's #prefix was verified by the carrier under the OFCA SMS Sender Registration Scheme. The organization behind it is registered, even though SMSGuard doesn't recognize this specific prefix."
        "blocklist.url_or_domain" ->
            "The link in this message matches an entry on the SMSGuard blocklist."
        else -> {
            val first = record.firedRuleIds.firstOrNull() ?: "unknown"
            "Rule fired: $first. The wording or links in this message match a known scam pattern."
        }
    }
}
