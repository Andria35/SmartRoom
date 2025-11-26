package com.example.smartroom;

import android.content.Context;
import android.content.SharedPreferences;

public class AccessibilityPrefs {

    private static final String PREFS_NAME = "accessibility_prefs";
    private static final String KEY_ACCESSIBILITY_MODE = "accessibility_mode";

    public static boolean isAccessibilityEnabled(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ACCESSIBILITY_MODE, false);
    }

    public static void setAccessibilityEnabled(Context context, boolean enabled) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ACCESSIBILITY_MODE, enabled).apply();
    }
}