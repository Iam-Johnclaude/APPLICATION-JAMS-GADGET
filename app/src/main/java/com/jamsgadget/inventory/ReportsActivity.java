package com.jamsgadget.inventory;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.jamsgadget.inventory.adapter.TopItemsAdapter;
import com.jamsgadget.inventory.model.Item;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ReportsActivity extends AppCompatActivity {

    private TextView tvTotalItems, tvTotalStock, tvTotalValue, tvPhoneCount, tvAccCount, tvWellStocked, tvLowStock, tvNoItems;
    private ProgressBar pbPhones, pbAcc;
    private RecyclerView rvTopItems;
    private TopItemsAdapter topItemsAdapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Use the correct layout based on available XML files
        setContentView(R.layout.activity_reports);

        db = FirebaseFirestore.getInstance();

        initViews();
        fetchReportsData();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        tvTotalItems = findViewById(R.id.tvTotalItems);
        tvTotalStock = findViewById(R.id.tvTotalStock);
        tvTotalValue = findViewById(R.id.tvTotalValue);
        tvPhoneCount = findViewById(R.id.tvPhoneCount);
        tvAccCount = findViewById(R.id.tvAccCount);
        tvWellStocked = findViewById(R.id.tvWellStocked);
        tvLowStock = findViewById(R.id.tvLowStock);
        tvNoItems = findViewById(R.id.tvNoItems);
        pbPhones = findViewById(R.id.pbPhones);
        pbAcc = findViewById(R.id.pbAcc);
        rvTopItems = findViewById(R.id.rvTopItems);

        if (rvTopItems != null) {
            rvTopItems.setLayoutManager(new LinearLayoutManager(this));
            topItemsAdapter = new TopItemsAdapter();
            rvTopItems.setAdapter(topItemsAdapter);
        }
    }

    private void fetchReportsData() {
        db.collection("items").addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;

            List<Item> allItems = new ArrayList<>();
            int totalStock = 0;
            double totalValue = 0;
            int phoneCount = 0;
            int accCount = 0;
            int wellStockedCount = 0;
            int lowStockCount = 0;

            for (QueryDocumentSnapshot doc : value) {
                Item item = doc.toObject(Item.class);
                item.setId(doc.getId());
                allItems.add(item);

                totalStock += item.getQuantity();
                totalValue += (item.getPrice() * item.getQuantity());

                if ("Smartphones & Tablets".equals(item.getCategory())) phoneCount++;
                else if ("Accessories & Peripherals".equals(item.getCategory())) accCount++;

                if (item.isLowStock()) lowStockCount++;
                else wellStockedCount++;
            }

            // Update Header Stats with null checks
            if (tvTotalItems != null) tvTotalItems.setText(String.valueOf(allItems.size()));
            if (tvTotalStock != null) tvTotalStock.setText(String.valueOf(totalStock));
            if (tvTotalValue != null) tvTotalValue.setText(formatCompactCurrency(totalValue));

            // Update Category Breakdown with null checks
            if (tvPhoneCount != null) tvPhoneCount.setText(String.valueOf(phoneCount));
            if (tvAccCount != null) tvAccCount.setText(String.valueOf(accCount));
            int totalItemsCount = allItems.size();
            if (pbPhones != null) pbPhones.setProgress(totalItemsCount > 0 ? (phoneCount * 100) / totalItemsCount : 0);
            if (pbAcc != null) pbAcc.setProgress(totalItemsCount > 0 ? (accCount * 100) / totalItemsCount : 0);

            // Update Stock Health with null checks
            if (tvWellStocked != null) tvWellStocked.setText(String.valueOf(wellStockedCount));
            if (tvLowStock != null) tvLowStock.setText(String.valueOf(lowStockCount));

            // Update Top Items with null checks
            if (topItemsAdapter != null) {
                Collections.sort(allItems, (o1, o2) -> Double.compare(o2.getPrice() * o2.getQuantity(), o1.getPrice() * o1.getQuantity()));
                topItemsAdapter.updateList(allItems);
            }
            if (tvNoItems != null) tvNoItems.setVisibility(allItems.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private String formatCompactCurrency(double value) {
        if (value >= 1000000) return String.format(Locale.getDefault(), "₱%.1fM", value / 1000000.0);
        if (value >= 1000) return String.format(Locale.getDefault(), "₱%.1fK", value / 1000.0);
        return String.format(Locale.getDefault(), "₱%.0f", value);
    }
}