package com.jamsgadget.inventory;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.jamsgadget.inventory.model.Item;
import com.jamsgadget.inventory.util.ThemeHelper;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

public class ItemDetailActivity extends AppCompatActivity {

    private String itemId;
    private FirebaseFirestore db;
    private Item currentItem;

    private ImageView imgItem;
    private TextView tvEmojiFallback, tvItemName, tvBrand, badgeCategory, badgeStock;
    private TextView tvPrice, tvQuantity, tvStockStatus, tvDescription, tvInfoCategory, tvDateAdded;
    private ProgressBar stockProgressBar;
    private MaterialButton btnViewSpecs, btnEdit, btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        itemId = getIntent().getStringExtra("itemId");
        if (itemId == null) {
            Toast.makeText(this, "Error: Item ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        initViews();
        setupToolbar();
        loadItemData();
    }

    private void initViews() {
        imgItem = findViewById(R.id.imgItem);
        tvEmojiFallback = findViewById(R.id.tvEmojiFallback);
        tvItemName = findViewById(R.id.tvItemName);
        tvBrand = findViewById(R.id.tvBrand);
        badgeCategory = findViewById(R.id.badgeCategory);
        badgeStock = findViewById(R.id.badgeStock);
        tvPrice = findViewById(R.id.tvPrice);
        tvQuantity = findViewById(R.id.tvQuantity);
        tvStockStatus = findViewById(R.id.tvStockStatus);
        tvDescription = findViewById(R.id.tvDescription);
        tvInfoCategory = findViewById(R.id.tvInfoCategory);
        tvDateAdded = findViewById(R.id.tvDateAdded);
        stockProgressBar = findViewById(R.id.stockProgressBar);
        btnViewSpecs = findViewById(R.id.btnViewSpecs);
        btnEdit = findViewById(R.id.btnEditDetail);
        btnDelete = findViewById(R.id.btnDeleteDetail);

        btnViewSpecs.setOnClickListener(v -> {
            if (currentItem != null && currentItem.getSpecs() != null) {
                SpecsBottomSheet bottomSheet = SpecsBottomSheet.newInstance(currentItem.getSpecs(), currentItem.getCategory());
                bottomSheet.show(getSupportFragmentManager(), "SpecsBottomSheet");
            }
        });

        btnEdit.setOnClickListener(v -> openEditActivity());
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadItemData() {
        db.collection("items").document(itemId).get().addOnSuccessListener(doc -> {
            currentItem = doc.toObject(Item.class);
            if (currentItem != null) {
                displayItemData(currentItem);
            } else {
                Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error loading item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void displayItemData(Item item) {
        tvItemName.setText(item.getName());
        tvBrand.setText(item.getBrand());
        tvPrice.setText(String.format(Locale.getDefault(), "₱%,.2f", item.getPrice()));
        tvQuantity.setText(String.format(Locale.getDefault(), "%d units", item.getQuantity()));
        
        // Normalize Category Display Name
        String displayCategory = item.getCategory();
        if ("Iphones & Tablets".equals(displayCategory) || "Smartphones & Tablets".equals(displayCategory)) {
            displayCategory = "Smartphones";
        } else if ("Accessories & Peripherals".equals(displayCategory)) {
            displayCategory = "Accessories";
        }
        
        tvInfoCategory.setText(displayCategory);
        badgeCategory.setText(displayCategory);

        // Description
        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            tvDescription.setText(item.getDescription());
            tvDescription.setTypeface(null, android.graphics.Typeface.NORMAL);
            tvDescription.setTextColor(Color.parseColor("#555555"));
        } else {
            tvDescription.setText("No description provided.");
            tvDescription.setTypeface(null, android.graphics.Typeface.ITALIC);
            tvDescription.setTextColor(Color.GRAY);
        }

        // Date
        if (item.getDateAdded() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            tvDateAdded.setText(sdf.format(item.getDateAdded().toDate()));
        }

        // Image
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            tvEmojiFallback.setVisibility(View.GONE);
            imgItem.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(item.getImageUrl())
                    .transform(new RoundedCorners(40)) 
                    .into(imgItem);
        } else {
            imgItem.setVisibility(View.GONE);
            tvEmojiFallback.setVisibility(View.VISIBLE);
            tvEmojiFallback.setText("Item");
        }

        // Stock Level Calculation
        int qty = item.getQuantity();
        int threshold = item.getLowStockThreshold();
        
        int maxForProgress = Math.max(threshold * 4, qty + 1);
        int progress = (qty * 100) / maxForProgress;
        stockProgressBar.setProgress(Math.min(100, progress));

        if (qty <= threshold) {
            badgeStock.setText("Low Stock");
            badgeStock.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
            tvStockStatus.setText("Low");
            tvStockStatus.setTextColor(Color.parseColor("#F44336"));
            stockProgressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
        } else if (progress < 50) {
            badgeStock.setText("In Stock");
            badgeStock.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            tvStockStatus.setText("Fair");
            tvStockStatus.setTextColor(Color.parseColor("#FF9800"));
            stockProgressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#FF9800")));
        } else {
            badgeStock.setText("In Stock");
            badgeStock.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            tvStockStatus.setText("Good");
            tvStockStatus.setTextColor(Color.parseColor("#4CAF50"));
            stockProgressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        }

        // Specs Visibility
        if (item.getSpecs() != null && !item.getSpecs().isEmpty()) {
            btnViewSpecs.setVisibility(View.VISIBLE);
        } else {
            btnViewSpecs.setVisibility(View.GONE);
        }
    }

    private void openEditActivity() {
        Intent intent = new Intent(this, AddEditItemActivity.class);
        intent.putExtra("itemId", itemId);
        intent.putExtra("mode", "edit");
        startActivity(intent);
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to permanently delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("items").document(itemId).delete().addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Item deleted.", Toast.LENGTH_SHORT).show();
                        finish();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_edit) {
            openEditActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItemData();
    }
}
