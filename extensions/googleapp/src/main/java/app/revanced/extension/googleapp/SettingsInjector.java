package app.revanced.extension.googleapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class SettingsInjector {
    private static final String GOOGLE_SETTINGS_ACTIVITY =
            "com.google.android.apps.search.googleapp.settingsui.SettingsActivity";
    private static final String ROW_TAG = "google_revanced_settings_row";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Map<Activity, Boolean> SCHEDULED =
            Collections.synchronizedMap(new WeakHashMap<>());

    private SettingsInjector() {
    }

    public static void onActivityResumed(Activity activity) {
        if (!GOOGLE_SETTINGS_ACTIVITY.equals(activity.getClass().getName())) {
            return;
        }
        synchronized (SCHEDULED) {
            if (SCHEDULED.containsKey(activity)) {
                return;
            }
            SCHEDULED.put(activity, Boolean.TRUE);
        }
        attempt(activity, 0);
    }

    public static void onActivityDestroyed(Activity activity) {
        SCHEDULED.remove(activity);
    }

    private static void attempt(Activity activity, int count) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            SCHEDULED.remove(activity);
            return;
        }
        if (inject(activity)) {
            return;
        }
        if (count < 40) {
            MAIN_HANDLER.postDelayed(() -> attempt(activity, count + 1), 250L);
        }
    }

    private static boolean inject(Activity activity) {
        int recyclerId = activity.getResources().getIdentifier(
                "recycler_view",
                "id",
                activity.getPackageName()
        );
        if (recyclerId == 0) {
            return false;
        }
        View recycler = activity.findViewById(recyclerId);
        if (recycler == null || !(recycler.getParent() instanceof ViewGroup)) {
            return false;
        }
        ViewGroup parent = (ViewGroup) recycler.getParent();
        for (int index = 0; index < parent.getChildCount(); index++) {
            if (ROW_TAG.equals(parent.getChildAt(index).getTag())) {
                return true;
            }
        }

        int rowHeight = dp(activity, 92);
        View row = createRow(activity);
        row.setTag(ROW_TAG);
        if (parent instanceof FrameLayout) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    rowHeight,
                    Gravity.BOTTOM
            );
            parent.addView(row, params);
        } else {
            parent.addView(row, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    rowHeight
            ));
        }
        recycler.setPadding(
                recycler.getPaddingLeft(),
                recycler.getPaddingTop(),
                recycler.getPaddingRight(),
                recycler.getPaddingBottom() + rowHeight
        );
        if (recycler instanceof ViewGroup) {
            ((ViewGroup) recycler).setClipToPadding(false);
        }
        return true;
    }

    private static View createRow(Activity activity) {
        LocalizedStrings strings = LocalizedStrings.current();
        int primary = themeColor(activity, android.R.attr.textColorPrimary, Color.WHITE);
        int secondary = themeColor(activity, android.R.attr.textColorSecondary, 0xffb0b0b0);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(activity, 24), dp(activity, 12), dp(activity, 24), dp(activity, 12));
        row.setLayoutDirection(strings.rightToLeft ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        row.setFocusable(true);
        row.setClickable(true);
        TypedValue selectable = new TypedValue();
        if (activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, selectable, true)) {
            row.setBackgroundResource(selectable.resourceId);
        }

        ImageView icon = new ImageView(activity);
        int iconId = activity.getResources().getIdentifier(
                "google_revanced_settings_icon",
                "drawable",
                activity.getPackageName()
        );
        if (iconId != 0) {
            icon.setImageResource(iconId);
            icon.setColorFilter(primary);
        }
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48));
        iconParams.setMarginEnd(dp(activity, 24));
        row.addView(icon, iconParams);

        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(activity);
        title.setText(strings.settingsTitle);
        title.setTextColor(primary);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        TextView summary = new TextView(activity);
        summary.setText(strings.settingsSummary);
        summary.setTextColor(secondary);
        summary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        summary.setPadding(0, dp(activity, 2), 0, 0);
        labels.addView(title);
        labels.addView(summary);
        row.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.setContentDescription(strings.settingsTitle + ". " + strings.settingsSummary);
        row.setOnClickListener(view -> activity.startActivity(
                new Intent().setClassName(
                        activity.getPackageName(),
                        GoogleAppReVancedSettingsActivity.class.getName()
                )
        ));
        return row;
    }

    private static int themeColor(Activity activity, int attribute, int fallback) {
        TypedValue value = new TypedValue();
        if (!activity.getTheme().resolveAttribute(attribute, value, true)) {
            return fallback;
        }
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return value.data;
        }
        try {
            return activity.getColor(value.resourceId);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
