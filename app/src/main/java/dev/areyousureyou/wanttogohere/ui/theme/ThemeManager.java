package com.auwimo.areyousure.ui.theme;

import android.content.Context;
import android.content.SharedPreferences;

public final class ThemeManager {
    private static final String PREFERENCES_NAME = "appearance";
    private static final String THEME_KEY = "theme_id";

    private ThemeManager() {
    }

    public static AppTheme getActiveTheme(Context context) {
        String savedId = preferences(context).getString(
                THEME_KEY, AppTheme.defaultTheme().id);
        AppTheme savedTheme = AppTheme.findById(savedId);
        return savedTheme == null ? AppTheme.defaultTheme() : savedTheme;
    }

    public static boolean setActiveTheme(Context context, String themeId) {
        if (AppTheme.findById(themeId) == null) {
            return false;
        }

        preferences(context).edit()
                .putString(THEME_KEY, themeId)
                .apply();
        return true;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
}
