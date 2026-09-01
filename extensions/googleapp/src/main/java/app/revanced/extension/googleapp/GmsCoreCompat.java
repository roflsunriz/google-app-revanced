package app.revanced.extension.googleapp;

import android.app.Application;
import android.os.Build;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public final class GmsCoreCompat {
    private static final String ORIGINAL_PACKAGE = "com.google.android.googlequicksearchbox";
    private static final String REVANCED_PACKAGE = "app.revanced.android.googleapp";

    private GmsCoreCompat() {
    }

    public static String getProcessName() {
        return asOriginalProcessName(readProcessName());
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
}
