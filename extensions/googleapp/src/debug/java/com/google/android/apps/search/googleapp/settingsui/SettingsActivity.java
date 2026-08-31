package com.google.android.apps.search.googleapp.settingsui;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.revanced.extension.googleapp.R;
import app.revanced.extension.googleapp.SettingsInjector;

public final class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout page = new FrameLayout(this);
        page.setBackgroundColor(Color.BLACK);

        LinearLayout list = new LinearLayout(this);
        list.setId(R.id.recycler_view);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(24), dp(32), dp(24), 0);
        list.addView(label("設定", 28));
        list.addView(label("プライバシーとセキュリティ", 18));
        list.addView(label("通知", 18));
        list.addView(label("Google アシスタント", 18));
        list.addView(label("Gemini", 18));
        list.addView(label("音声", 18));
        list.addView(label("その他の設定", 18));
        page.addView(list, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setContentView(page);
        SettingsInjector.onActivityResumed(this);
    }

    @Override
    protected void onDestroy() {
        SettingsInjector.onActivityDestroyed(this);
        super.onDestroy();
    }

    private TextView label(String value, int size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(Color.WHITE);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        text.setGravity(android.view.Gravity.CENTER_VERTICAL);
        text.setPadding(0, dp(12), 0, dp(12));
        return text;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
