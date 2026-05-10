package hk.smsguard.app.debug

import android.content.Context
import android.provider.Telephony
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hk.smsguard.app.rules.BlocklistLookup
import hk.smsguard.app.rules.DEFAULT_REGISTRY
import hk.smsguard.app.rules.DetectorContext
import hk.smsguard.app.rules.IncomingSms
import hk.smsguard.app.rules.Verdict
import hk.smsguard.app.rules.VerdictLabel
import hk.smsguard.app.rules.detect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SCAN_LIMIT = 500

private data class ScannedMessage(
    val id: Long,
    val sender: String,
    val bodyPreview: String,
    val receivedAtMillis: Long,
    val verdict: Verdict,
)

private data class ScanResult(
    val totalScanned: Int,
    val flagged: List<ScannedMessage>,
)

private sealed class ScanState {
    data object Idle : ScanState()
    data object Running : ScanState()
    data class Done(val result: ScanResult) : ScanState()
    data class Error(val message: String) : ScanState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ScanState>(ScanState.Idle) }

    LaunchedEffect(hasPermission) {
        if (hasPermission && state is ScanState.Idle) {
            state = ScanState.Running
            state = try {
                ScanState.Done(scanInbox(context))
            } catch (e: SecurityException) {
                ScanState.Error("Permission revoked: ${e.message}")
            } catch (e: Exception) {
                ScanState.Error("Scan failed: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan inbox (debug)") },
                navigationIcon = { TextButton(onClick = onClose) { Text("✕") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            if (!hasPermission) {
                NeedsPermission(onRequestPermission)
                return@Scaffold
            }

            when (val s = state) {
                ScanState.Idle, ScanState.Running -> ScanningView()
                is ScanState.Done -> ResultsView(
                    result = s.result,
                    onRescan = {
                        state = ScanState.Running
                        scope.launch {
                            state = try {
                                ScanState.Done(scanInbox(context))
                            } catch (e: Exception) {
                                ScanState.Error("Rescan failed: ${e.message}")
                            }
                        }
                    },
                )
                is ScanState.Error -> ErrorView(s.message, onRetry = {
                    state = ScanState.Running
                    scope.launch {
                        state = try {
                            ScanState.Done(scanInbox(context))
                        } catch (e: Exception) {
                            ScanState.Error("Retry failed: ${e.message}")
                        }
                    }
                })
            }
        }
    }
}

@Composable
private fun NeedsPermission(onRequest: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Read SMS permission required", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "This debug-only feature reads up to $SCAN_LIMIT messages from your inbox and runs the detector locally. Nothing is sent off-device.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRequest) { Text("Grant permission") }
        }
    }
}

@Composable
private fun ScanningView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Scanning your inbox…", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ResultsView(result: ScanResult, onRescan: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Scanned ${result.totalScanned} messages — ${result.flagged.size} flagged",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "These are messages already in your inbox that SMSGuard would have flagged. Use this to sanity-check the rules.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRescan) { Text("Scan again") }
        }
    }
    Spacer(Modifier.height(12.dp))

    if (result.flagged.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(
                "No flagged messages in the last ${result.totalScanned}. Either your inbox is clean or the rules need tuning.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items = result.flagged, key = { it.id }) { msg ->
                ScannedMessageRow(msg)
            }
        }
    }
}

@Composable
private fun ScannedMessageRow(msg: ScannedMessage) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                labelText(msg.verdict.label),
                style = MaterialTheme.typography.titleMedium,
                color = labelColor(msg.verdict.label),
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text("From ${msg.sender}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                msg.bodyPreview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${formatTime(msg.receivedAtMillis)} • score ${"%.2f".format(msg.verdict.score)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (msg.verdict.firedRuleIds.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Rules: ${msg.verdict.firedRuleIds.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Scan failed", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

private suspend fun scanInbox(context: Context): ScanResult = withContext(Dispatchers.IO) {
    val ctx = DetectorContext(registry = DEFAULT_REGISTRY, blocklist = BlocklistLookup.EMPTY)
    val flagged = mutableListOf<ScannedMessage>()
    var totalScanned = 0

    val projection = arrayOf(
        Telephony.Sms._ID,
        Telephony.Sms.ADDRESS,
        Telephony.Sms.BODY,
        Telephony.Sms.DATE,
    )

    context.contentResolver.query(
        Telephony.Sms.Inbox.CONTENT_URI,
        projection,
        null,
        null,
        "${Telephony.Sms.DATE} DESC",
    )?.use { cursor ->
        val idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
        val addrIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
        val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
        val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

        while (cursor.moveToNext() && totalScanned < SCAN_LIMIT) {
            totalScanned++
            val sender = cursor.getString(addrIdx) ?: continue
            val body = cursor.getString(bodyIdx) ?: continue
            if (body.isBlank()) continue

            val detail = detect(IncomingSms(senderId = sender, body = body), ctx)
            val label = detail.verdict.label
            if (label == VerdictLabel.NO_SIGNAL || label == VerdictLabel.TRUSTED) continue

            flagged += ScannedMessage(
                id = cursor.getLong(idIdx),
                sender = sender,
                bodyPreview = body.take(140).replace("\n", " "),
                receivedAtMillis = cursor.getLong(dateIdx),
                verdict = detail.verdict,
            )
        }
    }

    ScanResult(totalScanned = totalScanned, flagged = flagged)
}

private fun labelText(label: VerdictLabel): String = when (label) {
    VerdictLabel.HIGH_CONFIDENCE_PHISHING -> "Likely scam"
    VerdictLabel.LIKELY_SCAM -> "May be a scam"
    VerdictLabel.SUSPICIOUS -> "Suspicious"
    VerdictLabel.TRUSTED -> "Trusted"
    VerdictLabel.NO_SIGNAL -> "No signal"
}

@Composable
private fun labelColor(label: VerdictLabel): Color = when (label) {
    VerdictLabel.HIGH_CONFIDENCE_PHISHING -> MaterialTheme.colorScheme.error
    VerdictLabel.LIKELY_SCAM -> MaterialTheme.colorScheme.error
    VerdictLabel.SUSPICIOUS -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.onSurface
}

private fun formatTime(millis: Long): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return fmt.format(Date(millis))
}
