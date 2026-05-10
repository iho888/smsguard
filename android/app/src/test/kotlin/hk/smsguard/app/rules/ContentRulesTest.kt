package hk.smsguard.app.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentRulesTest {

    @Test
    fun `detects zh-HK account-frozen urgency`() {
        val hits = findContentRuleHits("您的帳戶將被凍結，請即驗證")
        assertTrue(hits.any { it.ruleId == "urgency.account_will_be_frozen" })
    }

    @Test
    fun `detects English account-suspended phrase`() {
        val hits = findContentRuleHits("Your account will be suspended unless you act now")
        assertTrue(hits.any { it.ruleId == "urgency.account_will_be_frozen" })
    }

    @Test
    fun `detects verify-account in English`() {
        val hits = findContentRuleHits("Please verify your account immediately.")
        assertTrue(hits.any { it.ruleId == "credential.verify_account" })
    }

    @Test
    fun `detects fake immigration department reference`() {
        val hits = findContentRuleHits("香港入境處通知：您的身份證已被凍結。")
        assertTrue(hits.any { it.category == ContentRuleCategory.FAKE_GOVERNMENT })
    }

    @Test
    fun `detects bit_ly shortener`() {
        val hits = findContentRuleHits("Click https://bit.ly/3abc123 to win!")
        assertTrue(hits.any { it.ruleId == "malicious.shortener_with_urgency" })
    }

    @Test
    fun `detects suspicious top TLD`() {
        val hits = findContentRuleHits("verify at http://hsbc-verify.top/auth now")
        assertTrue(hits.any { it.ruleId == "malicious.suspicious_tld" })
    }

    @Test
    fun `detects crypto transfer demand`() {
        val hits = findContentRuleHits("Transfer to bitcoin wallet bc1q... within 24h")
        assertTrue(hits.any { it.ruleId == "crypto.transfer_to_wallet" })
    }

    @Test
    fun `benign message has no hits`() {
        val hits = findContentRuleHits("Your dinner reservation tonight at 7pm is confirmed.")
        assertEquals(0, hits.size)
    }

    @Test
    fun `every rule id is unique`() {
        val ids = listAllRuleIds()
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `at least 10 rules registered`() {
        assertTrue(listAllRuleIds().size >= 10)
    }
}
