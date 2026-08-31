package dev.roflsunriz.googleapp

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AdNetworkClassifierTest {
    @Test
    fun `広告配信URLとホストを検出する`() {
        assertTrue(AdNetworkClassifier.isAdNetworkLiteral("https://googleads.g.doubleclick.net/pagead/ads"))
        assertTrue(AdNetworkClassifier.isAdNetworkLiteral(".googlesyndication.com"))
        assertTrue(AdNetworkClassifier.isAdNetworkLiteral("https://imasdk.googleapis.com/admob/sdkloader/native_video.html"))
    }

    @Test
    fun `通常のGoogle機能URLは遮断しない`() {
        assertFalse(AdNetworkClassifier.isAdNetworkLiteral("https://www.google.com/search?q=revanced"))
        assertFalse(AdNetworkClassifier.isAdNetworkLiteral("https://lens.google.com/"))
    }

}
