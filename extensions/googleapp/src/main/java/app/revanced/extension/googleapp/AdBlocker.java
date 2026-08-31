package app.revanced.extension.googleapp;

import android.net.Uri;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class AdBlocker {
    private static final String BLOCKED_URL = "https://blocked.invalid/";
    private static final Set<String> BLOCKED_HOSTS = new HashSet<>(Arrays.asList(
            "2mdn.net",
            "admob.com",
            "admob-gmats.uc.r.appspot.com",
            "adservice.google.com",
            "doubleclick.net",
            "googleadservices.com",
            "googleadsserving.cn",
            "googlesyndication.com",
            "googletagservices.com",
            "imasdk.googleapis.com"
    ));

    private AdBlocker() {
    }

    public static InetAddress[] getAllByName(String host) throws UnknownHostException {
        rejectHost(host);
        return InetAddress.getAllByName(host);
    }

    public static InetAddress getByName(String host) throws UnknownHostException {
        rejectHost(host);
        return InetAddress.getByName(host);
    }

    public static URLConnection openConnection(URL url) throws IOException {
        rejectUrl(url);
        return url.openConnection();
    }

    public static URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        rejectUrl(url);
        return url.openConnection(proxy);
    }

    public static Uri parseUri(String value) {
        return Uri.parse(sanitizeNetworkUrl(value));
    }

    public static String sanitizeNetworkUrl(String value) {
        return isBlockedUrl(value) ? BLOCKED_URL : value;
    }

    public static String sanitizeWebViewUrl(String value) {
        return isBlockedUrl(value) ? "about:blank" : value;
    }

    public static boolean isBlockedUrl(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.contains("/pagead/") || normalized.contains("/mads/")) {
            return true;
        }
        try {
            URI uri = URI.create(value);
            if (isBlockedHost(uri.getHost())) {
                return true;
            }
        } catch (IllegalArgumentException ignored) {
            // Some SDKs pass host names or regular expressions instead of complete URLs.
        }
        for (String blocked : BLOCKED_HOSTS) {
            if (normalized.equals(blocked) || normalized.contains("." + blocked)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBlockedHost(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        for (String blocked : BLOCKED_HOSTS) {
            if (normalized.equals(blocked) || normalized.endsWith("." + blocked)) {
                return true;
            }
        }
        return normalized.startsWith("adservice.google.");
    }

    private static void rejectHost(String host) throws UnknownHostException {
        if (isBlockedHost(host)) {
            throw new UnknownHostException("Blocked advertising host");
        }
    }

    private static void rejectUrl(URL url) throws UnknownHostException {
        if (url != null) {
            rejectHost(url.getHost());
        }
    }
}
