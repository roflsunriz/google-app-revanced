package app.revanced.extension.googleapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdBlockerTest {
    @Test
    public void blocksEveryKnownGoogleAdvertisingDomain() {
        assertTrue(AdBlocker.isBlockedUrl("https://googleads.g.doubleclick.net/pagead/ads"));
        assertTrue(AdBlocker.isBlockedUrl("https://pagead2.googlesyndication.com/pagead/ping"));
        assertTrue(AdBlocker.isBlockedUrl("https://www.googleadservices.com/pagead/conversion"));
        assertTrue(AdBlocker.isBlockedUrl("https://imasdk.googleapis.com/admob/sdkloader/native_video.html"));
        assertEquals("https://blocked.invalid/", AdBlocker.sanitizeNetworkUrl(
                "https://pubads.g.doubleclick.net/request"
        ));
    }

    @Test
    public void preservesNormalGoogleAppTraffic() {
        assertFalse(AdBlocker.isBlockedUrl("https://www.google.com/search?q=revanced"));
        assertEquals("https://lens.google.com/", AdBlocker.sanitizeNetworkUrl("https://lens.google.com/"));
    }
}
