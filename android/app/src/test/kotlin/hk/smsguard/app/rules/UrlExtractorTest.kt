package hk.smsguard.app.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlExtractorTest {

    @Test
    fun `lowercases scheme and host`() {
        assertEquals("https://example.com/Path", canonicalizeUrl("HTTPS://Example.COM/Path"))
    }

    @Test
    fun `strips default port http`() {
        assertEquals("http://example.com/x", canonicalizeUrl("http://example.com:80/x"))
    }

    @Test
    fun `strips default port https`() {
        assertEquals("https://example.com/x", canonicalizeUrl("https://example.com:443/x"))
    }

    @Test
    fun `strips tracking query params but keeps others`() {
        assertEquals(
            "https://example.com/x?id=42",
            canonicalizeUrl("https://example.com/x?utm_source=a&id=42&fbclid=z"),
        )
    }

    @Test
    fun `strips fragment`() {
        assertEquals("https://example.com/x", canonicalizeUrl("https://example.com/x#section"))
    }

    @Test
    fun `prepends http for bare domain`() {
        assertEquals("http://www.example.com/x", canonicalizeUrl("www.example.com/x"))
    }

    @Test
    fun `rejects non http schemes`() {
        assertNull(canonicalizeUrl("ftp://example.com"))
    }

    @Test
    fun `registrable domain for plain TLD`() {
        assertEquals("example.com", registrableDomainOf("https://www.example.com/x"))
    }

    @Test
    fun `registrable domain for hsbc com hk includes 3 labels`() {
        assertEquals("hsbc.com.hk", registrableDomainOf("https://www.hsbc.com.hk/login"))
    }

    @Test
    fun `extract single full url`() {
        val urls = extractUrls("Click here: https://bit.ly/3xyz123 to verify.")
        assertEquals(1, urls.size)
        assertEquals("https://bit.ly/3xyz123", urls[0].canonical)
        assertEquals("bit.ly", urls[0].registrableDomain)
    }

    @Test
    fun `extract www prefix url`() {
        val urls = extractUrls("Visit www.example.com for details.")
        assertEquals(1, urls.size)
        assertEquals("http://www.example.com/", urls[0].canonical)
    }

    @Test
    fun `dedupe canonical url`() {
        val urls = extractUrls(
            "Go https://Example.com/x and also HTTPS://example.COM/x?utm_source=a",
        )
        assertEquals(1, urls.size)
    }

    @Test
    fun `no url returns empty`() {
        assertTrue(extractUrls("Hello, no link here.").isEmpty())
    }

    @Test
    fun `extract suspicious top tld`() {
        val urls = extractUrls("Verify: http://hsbc-verify.top/auth right now")
        assertEquals(1, urls.size)
        assertEquals("hsbc-verify.top", urls[0].registrableDomain)
    }
}
