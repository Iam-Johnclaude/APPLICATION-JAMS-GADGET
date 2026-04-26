package com.jamsgadget.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.CalendarDay;

public class CalendarActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private MaterialCalendarView calendarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        calendarView = findViewById(R.id.calendarView);
        
        setupNavigationDrawer();
        
        // Fix for the crash: Ensure the toolbar exists if using setSupportActionBar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Fix: setup ivMenu ONLY if it exists in the layout
        View ivMenu = findViewById(R.id.ivMenu);
        if (ivMenu != null) {
            ivMenu.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
            });
        }
        
        if (calendarView != null) {
            calendarView.setOnDateChangedListener((widget, date, selected) -> {
                // Handle date selection here if needed
            });
        }
    }

    private void setupNavigationDrawer() {
        if (navView != null) {
            navView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                Intent intent = null;
                if (id == R.id.nav_dashboard) {
                    intent = new Intent(this, DashboardActivity.class);
                } else if (id == R.id.nav_inventory) {
                    intent = new Intent(this, InventoryActivity.class);
                } else if (id == R.id.nav_calendar) {
                    // Current
                } else if (id == R.id.nav_installments) {
                    intent = new Intent(this, InstallmentsActivity.class);
                } else if (id == R.id.nav_add_item) {
                    intent = new Intent(this, AddEditItemActivity.class);
                } else if (id == R.id.nav_reports) {
                    intent = new Intent(this, ReportsAnalyticsActivity.class);
                } else if (id == R.id.nav_profile) {
                    intent = new Intent(this, ProfileActivity.class);
                } else if (id == R.id.nav_logout) {
                    FirebaseAuth.getInstance().signOut();
                    intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                }
                
                if (intent != null) {
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                }
                
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            });
        }
    }
}