package hk.smsguard.app.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import hk.smsguard.app.ExplanationActivity
import hk.smsguard.app.R
import hk.smsguard.app.data.FlagRecord
import hk.smsguard.app.rules.VerdictLabel

object SmsGuardNotifier {
    const val CHANNEL_ID = "smsguard.scam_warning"
    private const val TAG_FLAG = "smsguard.flag"

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(true)
        }
        nm.createNotificationChannel(channel)
    }

    fun notifyFlag(context: Context, record: FlagRecord) {
        if (!shouldNotify(record.verdictLabel)) return

        val openIntent = Intent(context, ExplanationActivity::class.java).apply {
            putExtra(ExplanationActivity.EXTRA_FLAG_ID, record.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pending = PendingIntent.getActivity(
            context,
            record.id.toInt(),
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val title = headlineFor(record.verdictLabel)
        val text = buildString {
            append("From ")
            append(record.senderId)
            if (record.firstUrlHostHint != null) {
                append(" — link to ")
                append(record.firstUrlHostHint)
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(context.getColor(R.color.smsguard_red))
            .setColorized(false)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(TAG_FLAG, record.id.toInt(), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted on Android 13+. Silent — UI shows the missing permission.
        }
    }

    private fun shouldNotify(label: VerdictLabel): Boolean = when (label) {
        VerdictLabel.HIGH_CONFIDENCE_PHISHING,
        VerdictLabel.LIKELY_SCAM,
        VerdictLabel.SUSPICIOUS -> true
        VerdictLabel.TRUSTED,
        VerdictLabel.NO_SIGNAL -> false
    }

    private fun headlineFor(label: VerdictLabel): String = when (label) {
        VerdictLabel.HIGH_CONFIDENCE_PHISHING -> "⚠️ 極大機會係詐騙 / Likely scam SMS"
        VerdictLabel.LIKELY_SCAM -> "⚠️ 可能係詐騙 / This SMS may be a scam"
        VerdictLabel.SUSPICIOUS -> "⚠️ 短訊有啲可疑 / Suspicious SMS"
        else -> "短訊被標記 / SMS flagged"
    }
}
