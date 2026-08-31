package dev.roflsunriz.googleapp

internal object AdNetworkClassifier {
    private val blockedHosts = setOf(
        "2mdn.net",
        "admob.com",
        "adservice.google.com",
        "doubleclick.net",
        "googleadservices.com",
        "googleadsserving.cn",
        "googlesyndication.com",
        "googletagservices.com",
        "imasdk.googleapis.com",
    )

    fun isAdNetworkLiteral(value: String): Boolean {
        val normalized = value.lowercase()
        return blockedHosts.any { host ->
            normalized == host ||
                normalized.endsWith(".$host") ||
                normalized.contains("://$host") ||
                normalized.contains(".$host/") ||
                normalized.contains("\\.$host") ||
                normalized.contains("$host/")
        } || normalized.contains("/pagead/") || normalized.contains("/mads/")
    }

    fun replacement(value: String): String = when {
        value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true) -> "https://blocked.invalid/"
        value.startsWith('.') -> ".blocked.invalid"
        else -> "blocked.invalid"
    }
}
