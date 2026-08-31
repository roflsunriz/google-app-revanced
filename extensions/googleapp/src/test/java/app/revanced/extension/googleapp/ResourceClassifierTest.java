package app.revanced.extension.googleapp;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResourceClassifierTest {
    @Test
    public void recognizesAdAndPromotionResourceTokens() {
        assertTrue(ResourceClassifier.isAdName("ads_container"));
        assertTrue(ResourceClassifier.isAdName("ic_ad_badge_ja"));
        assertTrue(ResourceClassifier.isPromotionName("googleapp_discover_promo"));
        assertTrue(ResourceClassifier.isPromotionName("feature_promotional_card"));
    }

    @Test
    public void avoidsCommonSubstringFalsePositives() {
        assertFalse(ResourceClassifier.isAdName("content_padding"));
        assertFalse(ResourceClassifier.isAdName("add_language"));
        assertTrue(ResourceClassifier.isPromotionName("promoted_value"));
    }
}
