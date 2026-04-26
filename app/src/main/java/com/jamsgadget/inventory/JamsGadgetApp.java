package com.jamsgadget.inventory;

import android.app.Application;
import com.jamsgadget.inventory.util.ThemeHelper;

public class JamsGadgetApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeHelper.applyTheme(this);
    }
}