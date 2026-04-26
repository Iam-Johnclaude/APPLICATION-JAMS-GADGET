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
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.jamsgadget.inventory.util.ReminderWorker;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvItemsCount, tvAlertCount, tvPhonesCount, tvAccCount, tvStockStatus;
    private ImageView ivStockStatusIcon;
    private View btnAddItem, btnViewAll, btnInstallment, layoutNotifications, ivMenu, viewNotificationBadge;
    private View stockBanner;
    private DrawerLayout drawerLayout;
    private NavigationView navView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration statsListener;
    private ListenerRegistration userListener;
    private ListenerRegistration notificationsListener;

    private boolean hasLowStock = false;
    private boolean hasUpcomingDues = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupClickListeners();
        setupNavigationDrawer();
        startUserListener();
        startStatsListener();
        startNotificationsListener();
        
        setupAutomaticReminders();
    }

    private void setupAutomaticReminders() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest.Builder(
                ReminderWorker.class,
                24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DueDateReminders",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                reminderRequest);
    }

    private void initViews() {
        tvItemsCount = findViewById(R.id.tvItemsCount);
        tvAlertCount = findViewById(R.id.tvAlertCount);
        tvPhonesCount = findViewById(R.id.tvPhonesCount);
        tvAccCount = findViewById(R.id.tvAccCount);
        tvStockStatus = findViewById(R.id.tvStockStatus);
        ivStockStatusIcon = findViewById(R.id.ivStockStatusIcon);
        
        btnAddItem = findViewById(R.id.btnAddItem);
        btnViewAll = findViewById(R.id.btnViewAll);
        btnInstallment = findViewById(R.id.btnInstallment);
        layoutNotifications = findViewById(R.id.layoutNotifications);
        viewNotificationBadge = findViewById(R.id.viewNotificationBadge);
        ivMenu = findViewById(R.id.ivMenu);
        stockBanner = findViewById(R.id.stockBanner);
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        
        if (viewNotificationBadge != null) viewNotificationBadge.setVisibility(View.GONE);
    }

    private void startUserListener() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            View headerView = navView.getHeaderView(0);
            TextView tvNavEmail = headerView.findViewById(R.id.tvNavEmail);
            if (tvNavEmail != null) tvNavEmail.setText(user.getEmail());

            userListener = db.collection("users").document(uid).addSnapshotListener((documentSnapshot, error) -> {
                if (error != null) return;
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    String fullName = documentSnapshot.getString("fullName");
                    TextView tvNavName = headerView.findViewById(R.id.tvNavName);
                    if (tvNavName != null && fullName != null) tvNavName.setText(fullName);
                }
            });
        }
    }

    private void startStatsListener() {
        statsListener = db.collection("items").addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value != null) {
                updateDashboardStats(value.getDocuments());
            }
        });
    }

    private void startNotificationsListener() {
        notificationsListener = db.collection("installments")
                .whereEqualTo("status", "Active")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        checkUpcomingDues(value.getDocuments());
                    }
                });
    }

    private void checkUpcomingDues(List<DocumentSnapshot> documents) {
        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, 7);
        Date soon = cal.getTime();

        hasUpcomingDues = false;
        for (DocumentSnapshot doc : documents) {
            Timestamp nextDueTs = doc.getTimestamp("nextDueDate");
            if (nextDueTs != null) {
                Date nextDueDate = nextDueTs.toDate();
                if (nextDueDate.before(soon)) {
                    hasUpcomingDues = true;
                    break;
                }
            }
        }
        updateBadgeVisibility();
    }

    private void updateBadgeVisibility() {
        if (viewNotificationBadge != null) {
            viewNotificationBadge.setVisibility((hasLowStock || hasUpcomingDues) ? View.VISIBLE : View.GONE);
        }
    }

    private void updateDashboardStats(List<DocumentSnapshot> documents) {
        int totalItems = documents.size();
        int alertCount = 0;
        int phoneCount = 0;
        int accCount = 0;

        for (DocumentSnapshot doc : documents) {
            String category = doc.getString("category");
            Long qtyLong = doc.getLong("quantity");
            Long thresholdLong = doc.getLong("lowStockThreshold");
            
            int qty = qtyLong != null ? qtyLong.intValue() : 0;
            int threshold = thresholdLong != null ? thresholdLong.intValue() : 5;

            if (qty <= threshold) alertCount++;
            
            if (category != null) {
                if (category.equalsIgnoreCase("Smartphones & Tablets") || category.toLowerCase().contains("phone")) {
                    phoneCount++;
                } else if (category.equalsIgnoreCase("Accessories & Peripherals") || category.toLowerCase().contains("acc")) {
                    accCount++;
                }
            }
        }

        tvItemsCount.setText(String.valueOf(totalItems));
        tvAlertCount.setText(String.valueOf(alertCount));
        tvPhonesCount.setText(String.valueOf(phoneCount));
        tvAccCount.setText(String.valueOf(accCount));

        hasLowStock = alertCount > 0;
        updateBadgeVisibility();

        if (alertCount == 0) {
            stockBanner.setBackgroundResource(R.drawable.bg_stock_good);
            tvStockStatus.setText("No low stock alerts today");
            tvStockStatus.setTextColor(ContextCompat.getColor(this, R.color.brand_green));
            ivStockStatusIcon.setImageResource(R.drawable.ic_check);
            ivStockStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.brand_green));
        } else {
            stockBanner.setBackgroundResource(R.drawable.bg_stock_alert);
            tvStockStatus.setText(alertCount + " items with low stock!");
            tvStockStatus.setTextColor(ContextCompat.getColor(this, R.color.brand_red));
            ivStockStatusIcon.setImageResource(R.drawable.ic_alert);
            ivStockStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.brand_red));
        }
    }

    private void setupClickListeners() {
        btnAddItem.setOnClickListener(v -> startActivity(new Intent(this, AddEditItemActivity.class)));
        btnViewAll.setOnClickListener(v -> startActivity(new Intent(this, InventoryActivity.class)));
        btnInstallment.setOnClickListener(v -> startActivity(new Intent(this, InstallmentsActivity.class)));

        layoutNotifications.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        ivMenu.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    private void setupNavigationDrawer() {
        if (navView != null) {
            navView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                Intent intent = null;
                if (id == R.id.nav_dashboard) {
                    // Current
                } else if (id == R.id.nav_inventory) {
                    intent = new Intent(this, InventoryActivity.class);
                } else if (id == R.id.nav_calendar) {
                    intent = new Intent(this, CalendarActivity.class);
                } else if (id == R.id.nav_installments) {
                    intent = new Intent(this, InstallmentsActivity.class);
                } else if (id == R.id.nav_add_item) {
                    intent = new Intent(this, AddEditItemActivity.class);
                } else if (id == R.id.nav_reports) {
                    intent = new Intent(this, ReportsAnalyticsActivity.class);
                } else if (id == R.id.nav_profile) {
                    intent = new Intent(this, ProfileActivity.class);
                } else if (id == R.id.nav_logout) {
                    performLogout();
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

    private void performLogout() {
        // 1. Remove listeners FIRST to prevent PERMISSION_DENIED errors
        if (statsListener != null) statsListener.remove();
        if (userListener != null) userListener.remove();
        if (notificationsListener != null) notificationsListener.remove();
        
        // 2. Then sign out
        mAuth.signOut();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        if (statsListener != null) statsListener.remove();
        if (userListener != null) userListener.remove();
        if (notificationsListener != null) notificationsListener.remove();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
