package hk.smsguard.app.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SsrsVerifierTest {

    @Test
    fun `trusted registered prefix for hsbc`() {
        val outcome = checkSsrs(IncomingSms("#hsbc", "OTP 123456"), TEST_REGISTRY)
        assertTrue(outcome is SsrsCheckOutcome.TrustedRegisteredPrefix)
        assertEquals("hsbc", (outcome as SsrsCheckOutcome.TrustedRegisteredPrefix).observedPrefix)
    }

    @Test
    fun `prefix matches case-insensitively`() {
        val outcome = checkSsrs(IncomingSms("#HSBC", "OTP 123456"), TEST_REGISTRY)
        assertTrue(outcome is SsrsCheckOutcome.TrustedRegisteredPrefix)
    }

    @Test
    fun `unknown hash prefix when prefix not in registry`() {
        val outcome = checkSsrs(IncomingSms("#unknownbank", "Hello"), TEST_REGISTRY)
        assertTrue(outcome is SsrsCheckOutcome.UnknownHashPrefix)
        assertEquals("unknownbank", (outcome as SsrsCheckOutcome.UnknownHashPrefix).observedPrefix)
    }

    @Test
    fun `phishing when body claims HSBC but sender is not hsbc prefix`() {
        val outcome = checkSsrs(
            IncomingSms("+852 6123 4567", "【匯豐銀行】您的帳戶將被凍結，請點擊連結驗證"),
            TEST_REGISTRY,
        )
        assertTrue(outcome is SsrsCheckOutcome.PhishingClaimsOrgWithoutPrefix)
        assertEquals(
            "HSBC",
            (outcome as SsrsCheckOutcome.PhishingClaimsOrgWithoutPrefix).matchedOrg.canonicalName,
        )
    }

    @Test
    fun `phishing matches HSBC HK English alias`() {
        val outcome = checkSsrs(
            IncomingSms("+1 555 010 1234", "HSBC HK: Your account will be frozen. Verify here."),
            TEST_REGISTRY,
        )
        assertTrue(outcome is SsrsCheckOutcome.PhishingClaimsOrgWithoutPrefix)
    }

    @Test
    fun `no signal for benign SMS without org claim or prefix`() {
        val outcome = checkSsrs(
            IncomingSms("+852 9876 5432", "Hi, this is a reminder for your appointment at 3pm."),
            TEST_REGISTRY,
        )
        assertEquals(SsrsCheckOutcome.NoSignal, outcome)
    }

    @Test
    fun `verdict for trusted has score 1`() {
        val v = ssrsCheckToVerdict(SsrsCheckOutcome.TrustedRegisteredPrefix("hsbc"))
        assertNotNull(v)
        assertEquals(VerdictLabel.TRUSTED, v!!.label)
        assertEquals(1.0, v.score, 0.0001)
    }

    @Test
    fun `verdict for phishing has score at least 0_9`() {
        val v = ssrsCheckToVerdict(
            SsrsCheckOutcome.PhishingClaimsOrgWithoutPrefix(
                OrgPrefixMapping(
                    canonicalName = "HSBC",
                    aliasesZhHk = emptyList(),
                    aliasesEn = emptyList(),
                    expectedPrefix = "hsbc",
                    category = OrgCategory.BANK,
                    severity = Severity.HIGH,
                ),
            ),
        )
        assertNotNull(v)
        assertEquals(VerdictLabel.HIGH_CONFIDENCE_PHISHING, v!!.label)
        assertTrue(v.score >= 0.9)
    }

    @Test
    fun `verdict for no signal is null`() {
        assertNull(ssrsCheckToVerdict(SsrsCheckOutcome.NoSignal))
    }

    @Test
    fun `verdict for unknown hash prefix is null - carrier already gated`() {
        assertNull(ssrsCheckToVerdict(SsrsCheckOutcome.UnknownHashPrefix("csl")))
    }
}
