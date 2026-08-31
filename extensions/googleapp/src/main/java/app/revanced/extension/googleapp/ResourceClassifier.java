package app.revanced.extension.googleapp;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ResourceClassifier {
    private static final Pattern AD_TOKEN = Pattern.compile(
            "(^|_)(ad|ads|advert|advertisement|sponsor|sponsored)(_|$)"
    );
    private static final Pattern PROMO_TOKEN = Pattern.compile(
            "(^|_)(promo|promoted|promotion|promotional)(_|$)"
    );

    private ResourceClassifier() {
    }

    public static boolean isAdName(String value) {
        String normalized = normalize(value);
        return AD_TOKEN.matcher(normalized).find()
                || normalized.contains("admob")
                || normalized.contains("doubleclick")
                || normalized.contains("google_ads");
    }

    public static boolean isPromotionName(String value) {
        return PROMO_TOKEN.matcher(normalize(value)).find();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
