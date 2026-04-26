package com.jamsgadget.inventory;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.jamsgadget.inventory.adapter.InstallmentsAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class InstallmentsActivity extends AppCompatActivity {

    private static final String TAG = "InstallmentsActivity";
    private TextView tvTotalOutstanding, tvStatActiveCount, tvStatOverdueCount, tvStatPaidCount, tvCountLabel;
    private TextView tabActive, tabOverdue, tabPaid;
    private RecyclerView rvInstallments;
    private View layoutEmpty;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration installmentsListener;
    private InstallmentsAdapter adapter;
    private List<DocumentSnapshot> allInstallments = new ArrayList<>();
    private String currentFilter = "Active"; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_installments);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerView();
        setupFilters();
        updateTabStyles();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadInstallments(); // Start listener when activity becomes visible
    }

    private void initViews() {
        tvTotalOutstanding = findViewById(R.id.tvTotalOutstanding);
        tvStatActiveCount = findViewById(R.id.tvStatActiveCount);
        tvStatOverdueCount = findViewById(R.id.tvStatOverdueCount);
        tvStatPaidCount = findViewById(R.id.tvStatPaidCount);
        tvCountLabel = findViewById(R.id.tvCountLabel);
        rvInstallments = findViewById(R.id.rvInstallments);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        tabActive = findViewById(R.id.tabActive);
        tabOverdue = findViewById(R.id.tabOverdue);
        tabPaid = findViewById(R.id.tabPaid);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        View btnNew = findViewById(R.id.btnNew);
        if (btnNew != null) btnNew.setOnClickListener(v -> openAddInstallment());
        findViewById(R.id.fabAdd).setOnClickListener(v -> openAddInstallment());
    }

    private void setupRecyclerView() {
        rvInstallments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InstallmentsAdapter(new ArrayList<>(), new InstallmentsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(DocumentSnapshot document) {
                Intent intent = new Intent(InstallmentsActivity.this, InstallmentDetailActivity.class);
                intent.putExtra("installmentId", document.getId());
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(DocumentSnapshot document) {
                showDeleteConfirmation(document);
            }
        });
        rvInstallments.setAdapter(adapter);
    }

    private void showDeleteConfirmation(DocumentSnapshot document) {
        String customerName = document.getString("customerName");
        String itemName = document.getString("itemName");
        new AlertDialog.Builder(this)
                .setTitle("Delete Installment")
                .setMessage("Are you sure you want to delete the installment for " + (customerName != null ? customerName : "this customer") + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteInstallment(document.getId(), customerName, itemName))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteInstallment(String id, String customerName, String itemName) {
        db.collection("installments").document(id)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    deleteFromGoogleCalendar(customerName, itemName);
                    Toast.makeText(this, "Installment removed successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error removing installment", Toast.LENGTH_SHORT).show());
    }

    private void deleteFromGoogleCalendar(String customerName, String itemName) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) return;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                            InstallmentsActivity.this, 
                            Collections.singleton("https://www.googleapis.com/auth/calendar"));
                    credential.setSelectedAccountName(account.getEmail());

                    Calendar service = new Calendar.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            new GsonFactory(),
                            credential)
                            .setApplicationName("JamsGadget Inventory")
                            .build();

                    String query = "Installment Due: " + customerName;
                    Events events = service.events().list("primary").setQ(query).execute();

                    if (events.getItems() != null) {
                        for (Event event : events.getItems()) {
                            String summary = event.getSummary();
                            if (summary != null && summary.toLowerCase().contains(customerName.toLowerCase()) 
                                    && summary.toLowerCase().contains(itemName.toLowerCase())) {
                                service.events().delete("primary", event.getId()).execute();
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to delete calendar events", e);
                }
                return null;
            }
        }.execute();
    }

    private void setupFilters() {
        View.OnClickListener filterListener = v -> {
            int id = v.getId();
            if (id == R.id.tabActive) currentFilter = "Active";
            else if (id == R.id.tabOverdue) currentFilter = "Overdue";
            else if (id == R.id.tabPaid) currentFilter = "Paid";
            updateTabStyles();
            applyFilter();
        };
        tabActive.setOnClickListener(filterListener);
        tabOverdue.setOnClickListener(filterListener);
        tabPaid.setOnClickListener(filterListener);
    }

    private void updateTabStyles() {
        updateTabStyle(tabActive, "Active".equalsIgnoreCase(currentFilter));
        updateTabStyle(tabOverdue, "Overdue".equalsIgnoreCase(currentFilter));
        updateTabStyle(tabPaid, "Paid".equalsIgnoreCase(currentFilter));
    }

    private void updateTabStyle(TextView tab, boolean isSelected) {
        if (tab == null) return;
        tab.setBackgroundResource(isSelected ? R.drawable.bg_pill_filter_selected : R.drawable.bg_pill_filter_unselected);
        tab.setTextColor(ContextCompat.getColor(this, isSelected ? R.color.white : R.color.text_muted));
    }

    private void loadInstallments() {
        // Ensure old listener is gone
        if (installmentsListener != null) {
            installmentsListener.remove();
        }
        
        installmentsListener = db.collection("installments")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        if (mAuth.getCurrentUser() == null) return;
                        return;
                    }
                    if (value != null) {
                        allInstallments = value.getDocuments();
                        updateSummary();
                        applyFilter();
                    }
                });
    }

    private void updateSummary() {
        double totalOutstanding = 0;
        int active = 0, overdue = 0, paid = 0;

        for (DocumentSnapshot doc : allInstallments) {
            String status = doc.getString("status");
            String normalizedStatus = (status != null) ? status.trim() : "";
            if (normalizedStatus.equalsIgnoreCase("Active")) active++;
            else if (normalizedStatus.equalsIgnoreCase("Overdue")) overdue++;
            else if (normalizedStatus.equalsIgnoreCase("Paid")) paid++;

            if (!normalizedStatus.equalsIgnoreCase("Paid")) {
                double itemPrice = getDouble(doc, "itemPrice");
                double totalPaid = getDouble(doc, "totalPaid");
                totalOutstanding += Math.max(0, itemPrice - totalPaid);
            }
        }
        tvTotalOutstanding.setText(String.format(Locale.getDefault(), "₱%,.2f", totalOutstanding));
        tvStatActiveCount.setText(String.valueOf(active));
        tvStatOverdueCount.setText(String.valueOf(overdue));
        tvStatPaidCount.setText(String.valueOf(paid));
    }

    private void applyFilter() {
        List<DocumentSnapshot> filteredList = new ArrayList<>();
        for (DocumentSnapshot doc : allInstallments) {
            String status = doc.getString("status");
            if (status != null && status.equalsIgnoreCase(currentFilter)) filteredList.add(doc);
        }
        adapter.updateList(filteredList);
        if (tvCountLabel != null) tvCountLabel.setText(String.format(Locale.getDefault(), "%d installments", filteredList.size()));
        if (layoutEmpty != null) layoutEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private double getDouble(DocumentSnapshot doc, String field) {
        Double val = doc.getDouble(field);
        return val != null ? val : 0;
    }

    private void openAddInstallment() {
        startActivity(new Intent(this, AddInstallmentActivity.class));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (installmentsListener != null) {
            installmentsListener.remove();
            installmentsListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (installmentsListener != null) installmentsListener.remove();
        super.onDestroy();
    }
}
