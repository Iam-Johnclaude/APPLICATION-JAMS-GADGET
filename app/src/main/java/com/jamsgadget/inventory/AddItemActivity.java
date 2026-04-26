package com.jamsgadget.inventory;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddItemActivity extends AppCompatActivity {

    private ImageView ivItemPhoto;
    private TextView tvPhotoStatus, tvCharCount, tvQuantity, tvStepLabel, tvStepPercent;
    private EditText etItemName, etBrand, etDescription, etPrice, etThreshold;
    private ProgressBar progressBar;
    private TextView indicatorStep1, indicatorStep2;
    private TextView catSmartphone, catAccessory, catTablet, catLaptop;
    private TextView condBrandNew, condPreOwned, condRefurb;

    private String selectedCategory = "";
    private String selectedCondition = "Brand new";
    private int quantity = 0;
    private Uri selectedImageUri;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<Intent> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    ivItemPhoto.setImageURI(selectedImageUri);
                    ivItemPhoto.setPadding(0, 0, 0, 0);
                    tvPhotoStatus.setText("Photo selected");
                    tvPhotoStatus.setTextColor(ContextCompat.getColor(this, R.color.brand_gold));
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force light mode for this activity as requested by user
        getDelegate().setLocalNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();
        updateProgress();
    }

    private void initViews() {
        ivItemPhoto = findViewById(R.id.ivItemPhoto);
        tvPhotoStatus = findViewById(R.id.tvPhotoStatus);
        tvCharCount = findViewById(R.id.tvCharCount);
        tvQuantity = findViewById(R.id.tvQuantity);
        tvStepLabel = findViewById(R.id.tvStepLabel);
        tvStepPercent = findViewById(R.id.tvStepPercent);
        
        etItemName = findViewById(R.id.etItemName);
        etBrand = findViewById(R.id.etBrand);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        etThreshold = findViewById(R.id.etThreshold);
        
        progressBar = findViewById(R.id.progressBar);
        indicatorStep1 = findViewById(R.id.indicatorStep1);
        indicatorStep2 = findViewById(R.id.indicatorStep2);
        
        catSmartphone = findViewById(R.id.catSmartphone);
        catAccessory = findViewById(R.id.catAccessory);
        catTablet = findViewById(R.id.catTablet);
        catLaptop = findViewById(R.id.catLaptop);
        
        condBrandNew = findViewById(R.id.condBrandNew);
        condPreOwned = findViewById(R.id.condPreOwned);
        condRefurb = findViewById(R.id.condRefurb);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        // Photo Picker
        findViewById(R.id.btnChoosePhoto).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            photoPickerLauncher.launch(intent);
        });

        // Character Counter
        etItemName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCharCount.setText(s.length() + "/60");
                updateProgress();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Brand Chips
        LinearLayout brandChipsLayout = findViewById(R.id.brandChipsLayout);
        if (brandChipsLayout != null) {
            for (int i = 0; i < brandChipsLayout.getChildCount(); i++) {
                View child = brandChipsLayout.getChildAt(i);
                if (child instanceof TextView) {
                    child.setOnClickListener(v -> {
                        etBrand.setText(((TextView) v).getText().toString());
                        updateProgress();
                    });
                }
            }
        }

        // Category Selection
        View.OnClickListener catListener = v -> {
            resetCategoryStyles();
            v.setBackgroundResource(R.drawable.bg_category_selected);
            ((TextView) v).setTextColor(Color.WHITE);
            selectedCategory = ((TextView) v).getText().toString();
            updateProgress();
        };
        catSmartphone.setOnClickListener(catListener);
        catAccessory.setOnClickListener(catListener);
        catTablet.setOnClickListener(catListener);
        catLaptop.setOnClickListener(catListener);

        // Stepper
        findViewById(R.id.btnMinus).setOnClickListener(v -> {
            if (quantity > 0) {
                quantity--;
                tvQuantity.setText(String.valueOf(quantity));
            }
        });
        findViewById(R.id.btnPlus).setOnClickListener(v -> {
            quantity++;
            tvQuantity.setText(String.valueOf(quantity));
        });
        
        // Manual Quantity Entry
        tvQuantity.setOnClickListener(v -> showQuantityInputDialog());

        // Condition Toggle
        View.OnClickListener condListener = v -> {
            resetConditionStyles();
            v.setBackgroundResource(R.drawable.bg_category_selected);
            ((TextView) v).setTextColor(Color.WHITE);
            selectedCondition = ((TextView) v).getText().toString();
        };
        condBrandNew.setOnClickListener(condListener);
        condPreOwned.setOnClickListener(condListener);
        condRefurb.setOnClickListener(condListener);

        // Validation triggers
        etBrand.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateProgress(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        etPrice.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateProgress(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Save Button
        findViewById(R.id.btnSaveItem).setOnClickListener(v -> validateAndSave());
    }

    private void showQuantityInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_enter_quantity, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etQty = dialogView.findViewById(R.id.etDialogQuantity);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        etQty.setText(String.valueOf(quantity));
        etQty.setSelection(etQty.getText().length());
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String val = etQty.getText().toString();
            if (!val.isEmpty()) {
                try {
                    quantity = Integer.parseInt(val);
                    tvQuantity.setText(String.valueOf(quantity));
                } catch (NumberFormatException ignored) {}
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void resetCategoryStyles() {
        int defaultBg = R.drawable.bg_category_default;
        int textColor = Color.parseColor("#475569");
        catSmartphone.setBackgroundResource(defaultBg); catSmartphone.setTextColor(textColor);
        catAccessory.setBackgroundResource(defaultBg); catAccessory.setTextColor(textColor);
        catTablet.setBackgroundResource(defaultBg); catTablet.setTextColor(textColor);
        catLaptop.setBackgroundResource(defaultBg); catLaptop.setTextColor(textColor);
    }

    private void resetConditionStyles() {
        int defaultBg = R.drawable.bg_category_default;
        int textColor = Color.parseColor("#475569");
        condBrandNew.setBackgroundResource(defaultBg); condBrandNew.setTextColor(textColor);
        condPreOwned.setBackgroundResource(defaultBg); condPreOwned.setTextColor(textColor);
        condRefurb.setBackgroundResource(defaultBg); condRefurb.setTextColor(textColor);
    }

    private void updateProgress() {
        int filledFields = 0;
        if (!TextUtils.isEmpty(etItemName.getText())) filledFields++;
        if (!TextUtils.isEmpty(etBrand.getText())) filledFields++;
        if (!TextUtils.isEmpty(selectedCategory)) filledFields++;
        if (!TextUtils.isEmpty(etPrice.getText())) filledFields++;

        int percent = (filledFields * 100) / 4;
        progressBar.setProgress(percent);
        tvStepPercent.setText(percent + "%");

        if (percent < 100) {
            tvStepLabel.setText("Step 1 of 2 — Basic info");
            indicatorStep1.setBackgroundResource(R.drawable.bg_step_pill_active);
            indicatorStep1.setTextColor(ContextCompat.getColor(this, R.color.brand_navy));
            indicatorStep2.setBackgroundResource(R.drawable.bg_step_pill_inactive);
            indicatorStep2.setTextColor(Color.parseColor("#94A3B8"));
        } else {
            tvStepLabel.setText("Step 2 of 2 — Stock & price");
            indicatorStep1.setBackgroundResource(R.drawable.bg_step_pill_inactive);
            indicatorStep1.setTextColor(Color.parseColor("#94A3B8"));
            indicatorStep2.setBackgroundResource(R.drawable.bg_step_pill_active);
            indicatorStep2.setTextColor(ContextCompat.getColor(this, R.color.brand_navy));
        }
    }

    private void validateAndSave() {
        String name = etItemName.getText().toString().trim();
        String brand = etBrand.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String thresholdStr = etThreshold.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { Toast.makeText(this, "Please enter item name", Toast.LENGTH_SHORT).show(); return; }
        if (TextUtils.isEmpty(brand)) { Toast.makeText(this, "Please enter brand", Toast.LENGTH_SHORT).show(); return; }
        if (TextUtils.isEmpty(selectedCategory)) { Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show(); return; }
        if (TextUtils.isEmpty(priceStr)) { Toast.makeText(this, "Please enter price", Toast.LENGTH_SHORT).show(); return; }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int threshold = TextUtils.isEmpty(thresholdStr) ? 3 : Integer.parseInt(thresholdStr);

        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("brand", brand);
        item.put("category", selectedCategory);
        item.put("description", etDescription.getText().toString().trim());
        item.put("price", price);
        item.put("quantity", quantity);
        item.put("lowStockThreshold", threshold);
        item.put("condition", selectedCondition);
        item.put("dateAdded", Timestamp.now());
        if (selectedImageUri != null) {
            item.put("imageUrl", selectedImageUri.toString()); // Placeholder
        }

        db.collection("items").add(item)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Item saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error saving item", Toast.LENGTH_SHORT).show());
    }
}
