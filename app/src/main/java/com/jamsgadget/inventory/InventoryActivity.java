package com.jamsgadget.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.jamsgadget.inventory.adapter.InventoryAdapter;
import com.jamsgadget.inventory.model.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    private RecyclerView rvInventory;
    private InventoryAdapter adapter;
    private List<Item> itemList = new ArrayList<>();
    private List<Item> filteredList = new ArrayList<>();

    private EditText etSearch;
    private ChipGroup chipGroupFilter;
    private TextView tvGadgetCount;
    private View cardAlert;
    private TextView tvAlertMsg;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration itemsListener;
    
    private String currentCategory = "All";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerView();
        setupSearchAndFilters();
        // startItemsListener is called automatically by Lifecycle or manual call
        startItemsListener();
    }

    private void initViews() {
        rvInventory = findViewById(R.id.rvInventory);
        etSearch = findViewById(R.id.etSearch);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        tvGadgetCount = findViewById(R.id.tvGadgetCount);
        cardAlert = findViewById(R.id.cardAlert);
        tvAlertMsg = findViewById(R.id.tvAlertMsg);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAdd).setOnClickListener(v -> startActivity(new Intent(this, AddEditItemActivity.class)));
        findViewById(R.id.btnDismissAlert).setOnClickListener(v -> cardAlert.setVisibility(View.GONE));
        
        findViewById(R.id.btnSort).setOnClickListener(v -> {
            Collections.reverse(filteredList);
            adapter.notifyDataSetChanged();
        });
    }

    private void setupRecyclerView() {
        rvInventory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InventoryAdapter(filteredList, item -> {
            startActivity(new Intent(this, ItemDetailActivity.class)
                    .putExtra("itemId", item.getId()));
        });
        rvInventory.setAdapter(adapter);
    }

    private void setupSearchAndFilters() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase().trim();
                applyFilters();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) currentCategory = "All";
            else if (checkedId == R.id.chipPhones) currentCategory = "Phones";
            else if (checkedId == R.id.chipAccessories) currentCategory = "Accessories";
            else if (checkedId == R.id.chipLowStock) currentCategory = "Low Stock";
            applyFilters();
        });
    }

    private void startItemsListener() {
        // Use a continuous real-time listener without order-by timestamp constraints if failing
        // Or simply listen to the entire collection
        itemsListener = db.collection("items").addSnapshotListener((value, error) -> {
            if (error != null) {
                return;
            }
            if (value != null) {
                processItems(value.getDocuments());
            }
        });
    }

    private void processItems(List<DocumentSnapshot> documents) {
        itemList.clear();
        int lowStockCount = 0;
        for (DocumentSnapshot doc : documents) {
            Item item = doc.toObject(Item.class);
            if (item != null) {
                item.setId(doc.getId());
                itemList.add(item);
                if (item.isLowStock()) lowStockCount++;
            }
        }
        
        updateAlertBanner(lowStockCount);
        applyFilters();
    }

    private void updateAlertBanner(int lowStockCount) {
        if (lowStockCount > 0) {
            cardAlert.setVisibility(View.VISIBLE);
            tvAlertMsg.setText(lowStockCount + (lowStockCount == 1 ? " item is" : " items are") + " running low on stock!");
        } else {
            cardAlert.setVisibility(View.GONE);
        }
    }

    private void applyFilters() {
        filteredList.clear();
        for (Item item : itemList) {
            String name = item.getName() != null ? item.getName().toLowerCase() : "";
            String brand = item.getBrand() != null ? item.getBrand().toLowerCase() : "";
            boolean matchesSearch = name.contains(searchQuery) || brand.contains(searchQuery);
            
            boolean matchesCategory = false;
            String itemCat = item.getCategory() != null ? item.getCategory().toLowerCase() : "";
            
            if (currentCategory.equals("All")) {
                matchesCategory = true;
            } else if (currentCategory.equals("Phones")) {
                matchesCategory = itemCat.contains("phone") || itemCat.contains("tablet") || itemCat.contains("smartphone");
            } else if (currentCategory.equals("Accessories")) {
                matchesCategory = itemCat.contains("acc");
            } else if (currentCategory.equals("Low Stock")) {
                matchesCategory = item.isLowStock();
            }

            if (matchesSearch && matchesCategory) {
                filteredList.add(item);
            }
        }
        
        tvGadgetCount.setText(filteredList.size() + " gadgets total");
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Remove listener on stop to prevent leaks
        if (itemsListener != null) {
            itemsListener.remove();
            itemsListener = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restart listener on resume to ensure real-time updates when returning to activity
        if (itemsListener == null) {
            startItemsListener();
        }
    }

    @Override
    protected void onDestroy() {
        if (itemsListener != null) itemsListener.remove();
        super.onDestroy();
    }
}
