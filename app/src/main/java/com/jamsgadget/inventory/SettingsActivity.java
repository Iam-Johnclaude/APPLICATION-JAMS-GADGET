package com.jamsgadget.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.jamsgadget.inventory.util.ThemeHelper;

public class SettingsActivity extends AppCompatActivity {

    private ImageView checkLight, checkDark, checkSystem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        checkLight = findViewById(R.id.checkLight);
        checkDark = findViewById(R.id.checkDark);
        checkSystem = findViewById(R.id.checkSystem);

        updateCheckmarks();

        findViewById(R.id.rowLightTheme).setOnClickListener(v -> changeTheme(ThemeHelper.MODE_LIGHT));
        findViewById(R.id.rowDarkTheme).setOnClickListener(v -> changeTheme(ThemeHelper.MODE_DARK));
        findViewById(R.id.rowSystemTheme).setOnClickListener(v -> changeTheme(ThemeHelper.MODE_SYSTEM));
    }

    private void updateCheckmarks() {
        int currentMode = ThemeHelper.getSavedMode(this);
        checkLight.setVisibility(currentMode == ThemeHelper.MODE_LIGHT ? View.VISIBLE : View.GONE);
        checkDark.setVisibility(currentMode == ThemeHelper.MODE_DARK ? View.VISIBLE : View.GONE);
        checkSystem.setVisibility(currentMode == ThemeHelper.MODE_SYSTEM ? View.VISIBLE : View.GONE);
    }

    private void changeTheme(int mode) {
        if (ThemeHelper.getSavedMode(this) == mode) return;

        ThemeHelper.setTheme(this, mode);
        updateCheckmarks();

        // Restart to apply theme globally with transition
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}