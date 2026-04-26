package com.jamsgadget.inventory.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {
    public static final int MODE_LIGHT = 0;
    public static final int MODE_DARK = 1;
    public static final int MODE_SYSTEM = 2;

    private static final String PREFS_NAME = "jams_gadget_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    public static void applyTheme(Context context) {
        int mode = getSavedMode(context);
        updateAppCompatDelegate(mode);
    }

    public static void setTheme(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        updateAppCompatDelegate(mode);
    }

    private static void updateAppCompatDelegate(int mode) {
        switch (mode) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case MODE_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static int getSavedMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM);
    }

    public static boolean isDarkMode(Context context) {
        int mode = getSavedMode(context);
        if (mode == MODE_DARK) return true;
        if (mode == MODE_LIGHT) return false;

        int nightMode = context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }
}