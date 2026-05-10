package hk.smsguard.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-process flag log. Held in memory only — Phase 1 dogfood does not persist
 * across app process restarts. The privacy data-flow doc allows local
 * persistence of verdict + sender hash + URL host hint, which we will add
 * next session.
 */
object FlagStore {
    private const val MAX_ENTRIES = 200

    private val _flags = MutableStateFlow<List<FlagRecord>>(emptyList())
    val flags: StateFlow<List<FlagRecord>> = _flags.asStateFlow()

    @Synchronized
    fun add(record: FlagRecord) {
        val current = _flags.value
        val next = (listOf(record) + current).take(MAX_ENTRIES)
        _flags.value = next
    }

    @Synchronized
    fun clear() {
        _flags.value = emptyList()
    }

    fun byId(id: Long): FlagRecord? = _flags.value.firstOrNull { it.id == id }
}
