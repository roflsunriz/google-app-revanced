package app.revanced.extension.googleapp;

import android.content.Context;
import android.webkit.WebView;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class WebAdBlocker {
    private static final Map<WebView, Boolean> ATTACHED =
            Collections.synchronizedMap(new WeakHashMap<>());

    private WebAdBlocker() {
    }

    public static void attach(WebView webView) {
        synchronized (ATTACHED) {
            if (ATTACHED.containsKey(webView)) {
                inject(webView);
                return;
            }
            ATTACHED.put(webView, Boolean.TRUE);
        }
        webView.getViewTreeObserver().addOnGlobalLayoutListener(() -> inject(webView));
        inject(webView);
    }

    private static void inject(WebView webView) {
        Context context = webView.getContext();
        if (!Settings.hideWebAds(context) || !webView.isAttachedToWindow()) {
            return;
        }
        boolean hidePromotions = Settings.hidePromotions(context);
        String script = "(() => {"
                + "window.__googleRevancedConfig={hidePromotions:" + hidePromotions + "};"
                + "if(window.__googleRevancedInstalled){window.__googleRevancedRemove();return;}"
                + "window.__googleRevancedInstalled=true;"
                + "const selectors=['#tads','#tadsb','#bottomads','.commercial-unit-desktop-top',"
                + "'.ads-ad','[data-text-ad]','[data-ad-client]','[data-ad-slot]',"
                + "'[data-ad-format]','[data-google-query-id]','[id^=google_ads_]',"
                + "'iframe[src*=doubleclick]','iframe[src*=googlesyndication]',"
                + "'iframe[src*=googleadservices]'];"
                + "window.__googleRevancedRemove=()=>{"
                + "for(const s of selectors){for(const e of document.querySelectorAll(s)){e.remove();}}"
                + "if(window.__googleRevancedConfig.hidePromotions){"
                + "for(const a of document.querySelectorAll('a[href*=utm_campaign][href*=promo],a[href*=com.google.android.apps.bard]')){"
                + "const c=a.closest('article,[data-hveid],li,section,div');if(c)c.remove();else a.remove();}}};"
                + "new MutationObserver(window.__googleRevancedRemove).observe(document.documentElement,{childList:true,subtree:true});"
                + "window.__googleRevancedRemove();})();";
        try {
            webView.evaluateJavascript(script, null);
        } catch (RuntimeException ignored) {
            // A destroyed or non-JavaScript WebView is harmless; the next layout retries safely.
        }
    }
}
