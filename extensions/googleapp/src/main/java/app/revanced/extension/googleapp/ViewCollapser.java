package app.revanced.extension.googleapp;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class ViewCollapser {
    private static final Set<String> AD_LABELS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "ad", "ads", "sponsored", "advertisement", "広告", "赞助", "广告",
            "विज्ञापन", "anuncio", "patrocinado", "annonce", "sponsorisé",
            "إعلان", "مموّل", "anúncio", "patrocinado", "বিজ্ঞাপন",
            "реклама", "спонсировано", "اشتہار"
    )));
    private static final Map<Activity, ViewTreeObserver.OnGlobalLayoutListener> LISTENERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ViewCollapser() {
    }

    public static void onActivityResumed(Activity activity) {
        if (activity instanceof GoogleAppReVancedSettingsActivity) {
            return;
        }
        View root = activity.getWindow().getDecorView();
        scan(root, activity);
        synchronized (LISTENERS) {
            if (LISTENERS.containsKey(activity)) {
                return;
            }
            ViewTreeObserver.OnGlobalLayoutListener listener = () -> scan(root, activity);
            LISTENERS.put(activity, listener);
            root.getViewTreeObserver().addOnGlobalLayoutListener(listener);
        }
    }

    public static void onActivityDestroyed(Activity activity) {
        ViewTreeObserver.OnGlobalLayoutListener listener = LISTENERS.remove(activity);
        if (listener == null) {
            return;
        }
        View root = activity.getWindow().getDecorView();
        if (root.getViewTreeObserver().isAlive()) {
            root.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
        }
    }

    private static void scan(View view, Activity activity) {
        if (view instanceof WebView) {
            WebAdBlocker.attach((WebView) view);
        }
        String resourceName = resourceName(view);
        if (Settings.hideNativeAds(activity) && isNativeAdView(view, resourceName)) {
            collapse(resourceName.contains("badge") ? promotedContainer(view) : view);
            return;
        }
        if (Settings.hidePromotions(activity) && ResourceClassifier.isPromotionName(resourceName)) {
            collapse(promotedContainer(view));
            return;
        }
        if (Settings.hideNativeAds(activity) && isAdLabel(view)) {
            collapse(promotedContainer(view));
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = group.getChildCount() - 1; index >= 0; index--) {
                scan(group.getChildAt(index), activity);
            }
        }
    }

    private static boolean isNativeAdView(View view, String resourceName) {
        String className = view.getClass().getName().toLowerCase(Locale.ROOT);
        return className.contains("com.google.android.gms.ads")
                || className.contains("com.google.ads.interactivemedia")
                || ResourceClassifier.isAdName(resourceName)
                || ResourceClassifier.isAdName(String.valueOf(view.getTag()));
    }

    private static boolean isAdLabel(View view) {
        if (!(view instanceof TextView)) {
            return false;
        }
        CharSequence text = ((TextView) view).getText();
        return text != null && AD_LABELS.contains(text.toString().trim().toLowerCase(Locale.ROOT));
    }

    private static String resourceName(View view) {
        if (view.getId() == View.NO_ID) {
            return "";
        }
        try {
            return view.getResources().getResourceEntryName(view.getId());
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static View promotedContainer(View view) {
        View current = view;
        for (int depth = 0; depth < 7; depth++) {
            ViewParent parent = current.getParent();
            if (!(parent instanceof View)) {
                break;
            }
            String parentClass = parent.getClass().getName();
            if (parentClass.contains("RecyclerView") || parentClass.contains("ListView")) {
                return current;
            }
            if (current.isClickable() && current.getHeight() > view.getHeight()) {
                return current;
            }
            current = (View) parent;
        }
        return view;
    }

    private static void collapse(View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            params.width = 0;
            params.height = 0;
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) params).setMargins(0, 0, 0, 0);
            }
            view.setLayoutParams(params);
        }
        view.setMinimumWidth(0);
        view.setMinimumHeight(0);
        view.setPadding(0, 0, 0, 0);
        view.setVisibility(View.GONE);
    }
}
