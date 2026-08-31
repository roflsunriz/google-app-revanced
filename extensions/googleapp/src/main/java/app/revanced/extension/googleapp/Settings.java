package app.revanced.extension.googleapp;

import android.content.Context;
import android.content.SharedPreferences;

public final class Settings {
    private static final String FILE = "google_revanced_preferences";
    private static final String WEB_ADS = "hide_web_ads";
    private static final String PROMOTIONS = "hide_promotions";
    private static final String NATIVE_ADS = "hide_native_ads";

    private Settings() {
    }

    public static boolean hideWebAds(Context context) {
        return preferences(context).getBoolean(WEB_ADS, true);
    }

    public static boolean hidePromotions(Context context) {
        return preferences(context).getBoolean(PROMOTIONS, true);
    }

    public static boolean hideNativeAds(Context context) {
        return preferences(context).getBoolean(NATIVE_ADS, true);
    }

    public static void setHideWebAds(Context context, boolean value) {
        preferences(context).edit().putBoolean(WEB_ADS, value).apply();
    }

    public static void setHidePromotions(Context context, boolean value) {
        preferences(context).edit().putBoolean(PROMOTIONS, value).apply();
    }

    public static void setHideNativeAds(Context context, boolean value) {
        preferences(context).edit().putBoolean(NATIVE_ADS, value).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
