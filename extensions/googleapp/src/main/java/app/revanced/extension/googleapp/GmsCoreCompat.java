package app.revanced.extension.googleapp;

import android.app.Application;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Messenger;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONException;
import org.json.JSONObject;

public final class GmsCoreCompat {
    private static final String TAG = "GoogleReVancedGcm";
    private static final String ORIGINAL_PACKAGE = "com.google.android.googlequicksearchbox";
    private static final String REVANCED_PACKAGE = "app.revanced.android.googleapp";
    private static final String GMS_CORE_PACKAGE = "app.revanced.android.gms";
    private static final String REGISTER_ACTION = "com.google.android.c2dm.intent.REGISTER";
    private static final int CLOUD_MESSAGING_ATTEMPTS = 7;
    private static final AtomicBoolean CLOUD_MESSAGING_REQUEST_STARTED = new AtomicBoolean();

    private GmsCoreCompat() {
    }

    public static String getProcessName() {
        return asOriginalProcessName(readProcessName());
    }

    public static void requestCloudMessagingRegistration(Context context) {
        String processName = readProcessName();
        if (!shouldRequestCloudMessagingRegistration(context.getPackageName(), processName)
                || !CLOUD_MESSAGING_REQUEST_STARTED.compareAndSet(false, true)) {
            return;
        }

        Context applicationContext = context.getApplicationContext();
        Log.i(TAG, "Requesting ReVanced GmsCore Cloud Messaging registration");
        new Thread(
                () -> requestCloudMessagingTokenWithRetry(applicationContext),
                "GoogleReVanced-GCM"
        ).start();
    }

    private static void requestCloudMessagingTokenWithRetry(Context context) {
        long delayMillis = 1_000L;
        for (int attempt = 0; attempt < CLOUD_MESSAGING_ATTEMPTS; attempt++) {
            try {
                String sender = getRequiredStringResource(context, "gcm_defaultSenderId");
                String token = requestLegacyCloudMessagingToken(context, sender);
                storeCloudMessagingToken(context, sender, token);
                notifyCloudMessagingTokenWithRetry(token);
                return;
            } catch (Exception exception) {
                Log.w(
                        TAG,
                        "Cloud Messaging registration attempt " + (attempt + 1) + " failed",
                        exception
                );
                if (attempt == CLOUD_MESSAGING_ATTEMPTS - 1) {
                    break;
                }
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    CLOUD_MESSAGING_REQUEST_STARTED.set(false);
                    return;
                }
                delayMillis *= 2L;
            }
        }
        CLOUD_MESSAGING_REQUEST_STARTED.set(false);
    }

    private static void notifyCloudMessagingTokenWithRetry(String token) {
        long delayMillis = 1_000L;
        for (int attempt = 0; attempt < CLOUD_MESSAGING_ATTEMPTS; attempt++) {
            try {
                notifyCloudMessagingToken(token, null);
                return;
            } catch (Exception exception) {
                if (attempt == CLOUD_MESSAGING_ATTEMPTS - 1) {
                    Log.w(TAG, "Firebase token notification remained unavailable", exception);
                    return;
                }
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
                delayMillis *= 2L;
            }
        }
    }

    private static String requestLegacyCloudMessagingToken(Context context, String sender)
            throws Exception {
        HandlerThread handlerThread = new HandlerThread("GoogleReVanced-GCM-Reply");
        handlerThread.start();
        CountDownLatch responseReceived = new CountDownLatch(1);
        AtomicReference<String> registrationId = new AtomicReference<>();
        AtomicReference<String> registrationError = new AtomicReference<>();
        Messenger replyMessenger = new Messenger(new Handler(handlerThread.getLooper(), message -> {
            if (message.obj instanceof Intent) {
                Intent response = (Intent) message.obj;
                registrationId.set(response.getStringExtra("registration_id"));
                registrationError.set(response.getStringExtra("error"));
                responseReceived.countDown();
            }
            return true;
        }));

        try {
            int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent appIdentity = PendingIntent.getBroadcast(
                    context,
                    0,
                    new Intent().setPackage(context.getPackageName()),
                    pendingIntentFlags
            );
            Intent register = new Intent(REGISTER_ACTION)
                    .setPackage(GMS_CORE_PACKAGE)
                    .putExtra("app", appIdentity)
                    .putExtra("sender", sender)
                    .putExtra("subtype", sender)
                    .putExtra("scope", "*")
                    .putExtra("google.messenger", replyMessenger);
            putStringResourceExtra(context, register, "gmp_app_id", "google_app_id");
            ComponentName service = context.startService(register);
            if (service == null) {
                throw new IOException("Cloud Messaging registration service was not resolved");
            }
            if (!responseReceived.await(45, TimeUnit.SECONDS)) {
                throw new IOException("Cloud Messaging registration timed out");
            }
            String token = registrationId.get();
            if (token == null || token.isEmpty()) {
                throw new IOException("Cloud Messaging registration failed: " + registrationError.get());
            }
            return token;
        } finally {
            handlerThread.quitSafely();
        }
    }

    private static void putStringResourceExtra(
            Context context,
            Intent intent,
            String extraName,
            String resourceName
    ) {
        int identifier = context.getResources().getIdentifier(
                resourceName,
                "string",
                context.getPackageName()
        );
        if (identifier != 0) {
            intent.putExtra(extraName, context.getString(identifier));
        }
    }

    private static String getRequiredStringResource(Context context, String resourceName)
            throws IOException {
        int identifier = context.getResources().getIdentifier(
                resourceName,
                "string",
                context.getPackageName()
        );
        if (identifier == 0) {
            throw new IOException("Required Cloud Messaging resource is missing: " + resourceName);
        }
        return context.getString(identifier);
    }

    @SuppressWarnings("deprecation") // versionCode is required on Android 8.1 and older.
    private static void storeCloudMessagingToken(Context context, String sender, String token)
            throws PackageManager.NameNotFoundException, JSONException, IOException {
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        long versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                ? packageInfo.getLongVersionCode()
                : packageInfo.versionCode;
        JSONObject encodedToken = new JSONObject()
                .put("token", token)
                .put("appVersion", Long.toString(versionCode))
                .put("timestamp", System.currentTimeMillis());
        SharedPreferences preferences = context.getSharedPreferences(
                "com.google.android.gms.appid",
                Context.MODE_PRIVATE
        );
        if (!preferences.edit()
                .putString("|T|" + sender + "|*", encodedToken.toString())
                .commit()) {
            throw new IOException("Cloud Messaging token could not be persisted");
        }
    }

    /** Replaced by the GmsCore support patch with FirebaseMessaging.onNewToken dispatch. */
    private static void notifyCloudMessagingToken(String token, Object firebaseMessaging) {
    }

    private static String readProcessName() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/self/cmdline"))) {
            String processName = reader.readLine();
            if (processName == null) {
                return null;
            }
            int terminator = processName.indexOf('\0');
            return terminator >= 0 ? processName.substring(0, terminator) : processName;
        } catch (IOException ignored) {
            return null;
        }
    }

    static String asOriginalProcessName(String processName) {
        if (processName == null) {
            return null;
        }
        if (processName.equals(REVANCED_PACKAGE) || processName.startsWith(REVANCED_PACKAGE + ":")) {
            return ORIGINAL_PACKAGE + processName.substring(REVANCED_PACKAGE.length());
        }
        return processName;
    }

    static boolean shouldRequestCloudMessagingRegistration(String packageName, String processName) {
        return REVANCED_PACKAGE.equals(packageName)
                && (REVANCED_PACKAGE + ":googleapp").equals(processName);
    }
}
