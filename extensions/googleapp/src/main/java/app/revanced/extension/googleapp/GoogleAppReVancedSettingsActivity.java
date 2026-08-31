package app.revanced.extension.googleapp;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public final class GoogleAppReVancedSettingsActivity extends Activity {
    private int primaryColor;
    private int secondaryColor;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LocalizedStrings strings = LocalizedStrings.current();
        primaryColor = themeColor(android.R.attr.textColorPrimary, Color.WHITE);
        secondaryColor = themeColor(android.R.attr.textColorSecondary, 0xffb0b0b0);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(24), dp(12), dp(24), dp(32));
        page.setLayoutDirection(strings.rightToLeft ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);

        TextView close = text(strings.rightToLeft ? "›" : "‹", 36, primaryColor);
        close.setContentDescription(strings.close);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(view -> finish());
        page.addView(close, new LinearLayout.LayoutParams(dp(56), dp(56)));

        TextView title = text(strings.settingsTitle, 28, primaryColor);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(dp(8), dp(12), dp(8), dp(24));
        page.addView(title, titleParams);

        page.addView(switchRow(strings.sdkTitle, strings.sdkSummary, true, null));
        page.addView(switchRow(
                strings.webTitle,
                strings.webSummary,
                Settings.hideWebAds(this),
                value -> Settings.setHideWebAds(this, value)
        ));
        page.addView(switchRow(
                strings.promotionTitle,
                strings.promotionSummary,
                Settings.hidePromotions(this),
                value -> Settings.setHidePromotions(this, value)
        ));
        page.addView(switchRow(
                strings.nativeTitle,
                strings.nativeSummary,
                Settings.hideNativeAds(this),
                value -> Settings.setHideNativeAds(this, value)
        ));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(page);
        setTitle(strings.settingsTitle);
        setContentView(scroll);
    }

    private View switchRow(String title, String summary, boolean checked, ChangeListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(16), dp(8), dp(16));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = text(title, 18, primaryColor);
        TextView summaryView = text(summary, 14, secondaryColor);
        summaryView.setPadding(0, dp(4), 0, 0);
        labels.addView(titleView);
        labels.addView(summaryView);
        row.addView(labels, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Switch toggle = new Switch(this);
        toggle.setChecked(checked);
        toggle.setEnabled(listener != null);
        if (listener != null) {
            toggle.setOnCheckedChangeListener((button, value) -> listener.changed(value));
            row.setOnClickListener(view -> toggle.setChecked(!toggle.isChecked()));
        }
        row.addView(toggle, new LinearLayout.LayoutParams(dp(64), LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(color);
        return view;
    }

    private int themeColor(int attribute, int fallback) {
        TypedValue value = new TypedValue();
        if (!getTheme().resolveAttribute(attribute, value, true)) {
            return fallback;
        }
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return value.data;
        }
        try {
            return getColor(value.resourceId);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface ChangeListener {
        void changed(boolean value);
    }
}
