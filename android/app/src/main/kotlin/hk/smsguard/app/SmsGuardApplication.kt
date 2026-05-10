package hk.smsguard.app

import android.app.Application
import hk.smsguard.app.sms.SmsGuardNotifier

class SmsGuardApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SmsGuardNotifier.ensureChannel(this)
    }
}
