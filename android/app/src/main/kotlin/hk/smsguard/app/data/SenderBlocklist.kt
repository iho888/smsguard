package hk.smsguard.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory list of senders the user has explicitly blocked from the
 * "Block sender" action. SmsReceiver consults this before running detection.
 *
 * Phase 1 dogfood: in-memory only, lost on app process death — same
 * deliberate temporary choice as FlagStore. Persistence (DataStore/Room)
 * lands when we wire up the full flag history.
 */
object SenderBlocklist {
    private val _blocked = MutableStateFlow<Set<String>>(emptySet())
    val blocked: StateFlow<Set<String>> = _blocked.asStateFlow()

    @Synchronized
    fun block(senderId: String) {
        _blocked.value = _blocked.value + senderId.trim()
    }

    @Synchronized
    fun unblock(senderId: String) {
        _blocked.value = _blocked.value - senderId.trim()
    }

    fun isBlocked(senderId: String): Boolean = _blocked.value.contains(senderId.trim())
}
