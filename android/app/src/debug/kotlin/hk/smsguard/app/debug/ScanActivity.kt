package hk.smsguard.app.debug

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import hk.smsguard.app.SmsGuardTheme

class ScanActivity : ComponentActivity() {

    private var hasPermission by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hasPermission = readSmsGranted()

        setContent {
            SmsGuardTheme {
                Surface(modifier = Modifier, color = MaterialTheme.colorScheme.background) {
                    ScanScreen(
                        hasPermission = hasPermission,
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.READ_SMS) },
                        onClose = { finish() },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasPermission = readSmsGranted()
    }

    private fun readSmsGranted(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
}
