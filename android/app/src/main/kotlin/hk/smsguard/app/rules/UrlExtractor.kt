package hk.smsguard.app.rules

import java.net.URI
import java.net.URISyntaxException

private val URL_PATTERN = Regex(
    "\\b((?:https?://|www\\.)[^\\s<>\"'()]+|(?:[a-z0-9-]+\\.)+[a-z]{2,}(?:/[^\\s<>\"'()]*)?)",
    RegexOption.IGNORE_CASE,
)

private val EXPLICIT_SCHEME_PATTERN = Regex("^([a-z][a-z0-9+.-]*):", RegexOption.IGNORE_CASE)

private val TRACKING_QUERY_KEYS: Set<String> = setOf(
    "utm_source",
    "utm_medium",
    "utm_campaign",
    "utm_term",
    "utm_content",
    "gclid",
    "fbclid",
    "mc_cid",
    "mc_eid",
    "_ga",
)

private val KNOWN_MULTIPART_TLDS: Set<String> = setOf(
    "co.uk",
    "co.jp",
    "co.kr",
    "com.hk",
    "org.hk",
    "gov.hk",
    "edu.hk",
    "net.hk",
    "idv.hk",
    "com.cn",
    "com.tw",
    "com.au",
    "com.sg",
)

fun extractUrls(body: String): List<ExtractedUrl> {
    val seen = mutableSetOf<String>()
    val out = mutableListOf<ExtractedUrl>()
    for (match in URL_PATTERN.findAll(body)) {
        val raw = match.value
        val canonical = canonicalizeUrl(raw) ?: continue
        if (canonical in seen) continue
        seen += canonical
        out += ExtractedUrl(raw = raw, canonical = canonical, registrableDomain = registrableDomainOf(canonical))
    }
    return out
}

fun canonicalizeUrl(raw: String): String? {
    var candidate = raw.trim()
    if (candidate.isEmpty()) return null

    val schemeMatch = EXPLICIT_SCHEME_PATTERN.find(candidate)
    if (schemeMatch != null) {
        val scheme = schemeMatch.groupValues[1].lowercase()
        if (scheme != "http" && scheme != "https") return null
    } else {
        candidate = "http://$candidate"
    }

    val uri: URI = try {
        URI(candidate)
    } catch (_: URISyntaxException) {
        return null
    }

    val scheme = uri.scheme?.lowercase() ?: return null
    if (scheme != "http" && scheme != "https") return null

    val host = uri.host?.lowercase() ?: return null
    if (host.isEmpty()) return null

    val port = uri.port
    val portPart = when {
        port == -1 -> ""
        scheme == "http" && port == 80 -> ""
        scheme == "https" && port == 443 -> ""
        else -> ":$port"
    }

    val pathPart = uri.rawPath ?: ""
    val effectivePath = if (pathPart.isEmpty()) "/" else pathPart
    val queryPart = stripTrackingQuery(uri.rawQuery)

    return buildString {
        append(scheme)
        append("://")
        append(host)
        append(portPart)
        append(effectivePath)
        if (queryPart.isNotEmpty()) {
            append('?')
            append(queryPart)
        }
    }
}

fun registrableDomainOf(canonicalUrl: String): String {
    val host = try {
        URI(canonicalUrl).host?.lowercase() ?: return ""
    } catch (_: URISyntaxException) {
        return ""
    }
    if (host.isEmpty()) return ""

    val labels = host.split('.')
    if (labels.size < 2) return host

    val lastTwo = labels.takeLast(2).joinToString(".")
    if (labels.size >= 3 && lastTwo in KNOWN_MULTIPART_TLDS) {
        return labels.takeLast(3).joinToString(".")
    }
    return lastTwo
}

private fun stripTrackingQuery(rawQuery: String?): String {
    if (rawQuery.isNullOrEmpty()) return ""
    val kept = rawQuery.split('&').filter { pair ->
        if (pair.isEmpty()) return@filter false
        val key = pair.substringBefore('=').lowercase()
        key !in TRACKING_QUERY_KEYS
    }
    return kept.joinToString("&")
}
