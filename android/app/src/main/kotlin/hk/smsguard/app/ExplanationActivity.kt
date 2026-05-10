package hk.smsguard.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import hk.smsguard.app.data.FlagStore
import hk.smsguard.app.ui.ExplanationScreen

class ExplanationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val flagId = intent.getLongExtra(EXTRA_FLAG_ID, -1L)
        val record = if (flagId >= 0) FlagStore.byId(flagId) else null

        setContent {
            SmsGuardTheme {
                Surface(modifier = Modifier, color = MaterialTheme.colorScheme.background) {
                    ExplanationScreen(record = record, onClose = { finish() })
                }
            }
        }
    }

    companion object {
        const val EXTRA_FLAG_ID = "flag_id"

        fun start(context: Context, flagId: Long) {
            val intent = Intent(context, ExplanationActivity::class.java).apply {
                putExtra(EXTRA_FLAG_ID, flagId)
            }
            context.startActivity(intent)
        }
    }
}
