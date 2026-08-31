package dev.roflsunriz.googleapp

internal object ResourceClassifier {
    private val adToken = Regex("(^|_)(ad|ads|advert|advertisement|sponsor|sponsored)(_|$)")
    private val promoToken = Regex("(^|_)(promo|promoted|promotion|promotional)(_|$)")

    fun isAdName(name: String): Boolean {
        val normalized = normalize(name)
        return adToken.containsMatchIn(normalized) ||
            normalized.contains("admob") ||
            normalized.contains("doubleclick") ||
            normalized.contains("google_ads")
    }

    fun isPromotionName(name: String): Boolean = promoToken.containsMatchIn(normalize(name))

    fun shouldCollapseLayout(name: String): Boolean = isAdName(name) || isPromotionName(name)

    private fun normalize(name: String) = name.lowercase().replace('-', '_')
}
