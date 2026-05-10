package hk.smsguard.app.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectorTest {

    private val ctx = DetectorContext(registry = TEST_REGISTRY, blocklist = BlocklistLookup.EMPTY)

    @Test
    fun `trusted registered prefix yields trusted verdict`() {
        val r = detect(IncomingSms("#hsbc", "Your one-time password is 123456"), ctx)
        assertEquals(VerdictLabel.TRUSTED, r.verdict.label)
    }

    @Test
    fun `body claims hsbc without prefix yields high confidence phishing`() {
        val r = detect(
            IncomingSms("+852 6123 4567", "【匯豐銀行】您的帳戶將被凍結，請即驗證"),
            ctx,
        )
        assertEquals(VerdictLabel.HIGH_CONFIDENCE_PHISHING, r.verdict.label)
    }

    @Test
    fun `urgency plus credential plus shortener escalates to likely scam`() {
        val r = detect(
            IncomingSms("+852 6555 1234", "Your account expires in 6 hours. Update your details now: https://tinyurl.com/abc123"),
            ctx,
        )
        val rank = listOf(
            VerdictLabel.NO_SIGNAL,
            VerdictLabel.SUSPICIOUS,
            VerdictLabel.LIKELY_SCAM,
            VerdictLabel.HIGH_CONFIDENCE_PHISHING,
        )
        assertTrue(rank.indexOf(r.verdict.label) >= rank.indexOf(VerdictLabel.LIKELY_SCAM))
    }

    @Test
    fun `crypto remit plus urgency yields likely scam or worse`() {
        val r = detect(
            IncomingSms("+44 7700 900123", "You owe HKD 10,000. Please transfer to bitcoin wallet within 24 hours or face arrest."),
            ctx,
        )
        val rank = listOf(
            VerdictLabel.NO_SIGNAL,
            VerdictLabel.SUSPICIOUS,
            VerdictLabel.LIKELY_SCAM,
            VerdictLabel.HIGH_CONFIDENCE_PHISHING,
        )
        assertTrue(rank.indexOf(r.verdict.label) >= rank.indexOf(VerdictLabel.LIKELY_SCAM))
    }

    @Test
    fun `blocklist hit escalates verdict`() {
        val withBlocklist = DetectorContext(
            registry = TEST_REGISTRY,
            blocklist = object : BlocklistLookup {
                override fun isBlockedUrlCanonical(canonical: String): Boolean =
                    canonical == "https://shady.example/x"
                override fun isBlockedDomain(registrableDomain: String): Boolean = false
            },
        )
        val r = detect(
            IncomingSms("+852 9999 0000", "Hi click https://shady.example/x"),
            withBlocklist,
        )
        assertTrue(r.verdict.label == VerdictLabel.LIKELY_SCAM ||
            r.verdict.label == VerdictLabel.HIGH_CONFIDENCE_PHISHING)
        assertTrue(r.verdict.firedRuleIds.any { it.startsWith("blocklist.") })
    }

    @Test
    fun `benign message returns no signal`() {
        val r = detect(
            IncomingSms("+852 2345 6789", "Reminder: your dentist appointment is tomorrow at 10am."),
            ctx,
        )
        assertEquals(VerdictLabel.NO_SIGNAL, r.verdict.label)
    }

    @Test
    fun `unknown hash prefix on benign body returns no signal`() {
        val r = detect(
            IncomingSms("#csl", "CSL: Your monthly bill of HKD 388.00 is now ready. Thank you."),
            ctx,
        )
        assertEquals(VerdictLabel.NO_SIGNAL, r.verdict.label)
    }
}
