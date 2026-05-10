package hk.smsguard.app.ui

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent
import hk.smsguard.app.BuildConfig
import hk.smsguard.app.ExplanationActivity
import hk.smsguard.app.data.FlagRecord
import hk.smsguard.app.data.FlagStore
import hk.smsguard.app.rules.VerdictLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PermissionState(val receiveSms: Boolean, val postNotifications: Boolean) {
    val allGranted: Boolean get() = receiveSms && postNotifications
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    permissionState: PermissionState,
    onRequestPermissions: () -> Unit,
) {
    val flags by FlagStore.flags.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMSGuard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            if (!permissionState.allGranted) {
                PermissionBanner(state = permissionState, onGrant = onRequestPermissions)
                Spacer(Modifier.height(16.dp))
            } else {
                ActiveBanner()
                Spacer(Modifier.height(16.dp))
            }

            if (BuildConfig.DEBUG) {
                Button(
                    onClick = {
                        val intent = Intent("hk.smsguard.app.action.SCAN_INBOX_DEBUG").apply {
                            setPackage(context.packageName)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Scan existing inbox (debug)")
                }
                Spacer(Modifier.height(16.dp))
            }

            Text(
                "Recent flags",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))

            if (flags.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Text(
                        "No flagged SMS yet. SMSGuard runs silently — you'll get a notification if anything looks suspicious.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = flags, key = { it.id }) { record ->
                        FlagRow(record = record, onClick = {
                            ExplanationActivity.start(context, record.id)
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionBanner(state: PermissionState, onGrant: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "SMSGuard needs permissions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            val missing = buildList {
                if (!state.receiveSms) add("• Receive SMS — to read incoming messages locally")
                if (!state.postNotifications) add("• Show notifications — to alert you about scams")
            }
            for (line in missing) Text(line, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onGrant) { Text("Grant permissions") }
        }
    }
}

@Composable
private fun ActiveBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "SMSGuard is active",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Watching incoming SMS for scam patterns. Your messages never leave your phone.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun FlagRow(record: FlagRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                labelText(record.verdictLabel),
                style = MaterialTheme.typography.titleMedium,
                color = labelColor(record.verdictLabel),
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "From ${record.senderId}",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (record.firstUrlHostHint != null) {
                Text(
                    "Link host: ${record.firstUrlHostHint}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                formatTime(record.receivedAtMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
