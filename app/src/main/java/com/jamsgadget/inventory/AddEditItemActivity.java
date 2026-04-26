package com.jamsgadget.inventory;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddEditItemActivity extends AppCompatActivity {

    // Basic info
    private TextInputEditText etItemName, etBrand, etDescription;
    private ChipGroup categoryChipGroup, brandChipGroup;
    private TextView tvCategoryError, tvStepLabel, tvStepPercent;
    private ProgressBar progressStep;

    // Photo
    private ImageView imgPreview;
    private Button btnPickImage, btnRemoveImage;
    private TextView tvUploadStatus;
    private Uri selectedImageUri = null;
    private String existingImageUrl = null;

    // Stock & Pricing
    private TextInputEditText etPrice, etLowStockThreshold;
    private TextView tvQuantityDisplay, tvStockStatusMsg, tvStockStatusIcon;
    private Button btnDecrement, btnIncrement;
    private com.google.android.material.card.MaterialCardView cardStockStatus;

    // Spec cards
    private com.google.android.material.card.MaterialCardView cardSpecsSmartphone, cardSpecsAccessory;

    // Smartphone spec fields
    private TextInputEditText etDisplay, etRam, etStorage, etBattery,
            etProcessor, etOs, etCamera,
            etSpecColorPhone, etConditionPhone;

    // Accessory spec fields
    private TextInputEditText etType, etCompatible, etConnectivity, etWattage,
            etSpecColorAcc, etWarranty, etConditionAcc;

    // Action
    private Button btnSave, btnDelete;

    // State
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String itemId = null;
    private boolean isEditMode = false;
    private int currentQuantity = 0;

    // Cloudinary
    private static final String CLOUDINARY_CLOUD_NAME = "dutgjjzqa";
    private static final String CLOUDINARY_UPLOAD_PRESET = "jamsgadget_preset";
    private static final String CLOUDINARY_UPLOAD_URL =
            "https://api.cloudinary.com/v1_1/" + CLOUDINARY_CLOUD_NAME + "/image/upload";

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imgPreview.setImageURI(uri);
                    imgPreview.setPadding(0, 0, 0, 0);
                    imgPreview.setImageTintList(null); // Clear placeholder tint
                    btnRemoveImage.setVisibility(View.VISIBLE);
                    tvUploadStatus.setText("Photo selected — will upload on save");
                    tvUploadStatus.setTextColor(ContextCompat.getColor(this, R.color.brand_blue));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force Light Mode for this specific activity to ensure white background
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_item);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupToolbar();
        setupBrandChips();
        setupCategoryChips();
        setupQuantityStepper();
        setupStockPreview();
        setupImagePicker();
        checkEditMode();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        // Toolbar info
        tvStepLabel = findViewById(R.id.tvStepLabel);
        tvStepPercent = findViewById(R.id.tvStepPercent);
        progressStep = findViewById(R.id.progressStep);

        // Photo
        imgPreview = findViewById(R.id.imgPreview);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnRemoveImage = findViewById(R.id.btnRemoveImage);
        tvUploadStatus = findViewById(R.id.tvUploadStatus);

        // Basic Info
        etItemName = findViewById(R.id.etItemName);
        etBrand = findViewById(R.id.etBrand);
        brandChipGroup = findViewById(R.id.brandChipGroup);
        categoryChipGroup = findViewById(R.id.categoryChipGroup);
        tvCategoryError = findViewById(R.id.tvCategoryError);
        etDescription = findViewById(R.id.etDescription);

        // Spec Cards
        cardSpecsSmartphone = findViewById(R.id.cardSpecsSmartphone);
        cardSpecsAccessory = findViewById(R.id.cardSpecsAccessory);

        // Smartphone Specs
        etDisplay = findViewById(R.id.etDisplay);
        etRam = findViewById(R.id.etRam);
        etStorage = findViewById(R.id.etStorage);
        etBattery = findViewById(R.id.etBattery);
        etProcessor = findViewById(R.id.etProcessor);
        etOs = findViewById(R.id.etOs);
        etCamera = findViewById(R.id.etCamera);
        etSpecColorPhone = findViewById(R.id.etSpecColorPhone);
        etConditionPhone = findViewById(R.id.etConditionPhone);

        // Accessory Specs
        etType = findViewById(R.id.etType);
        etCompatible = findViewById(R.id.etCompatible);
        etConnectivity = findViewById(R.id.etConnectivity);
        etWattage = findViewById(R.id.etWattage);
        etSpecColorAcc = findViewById(R.id.etSpecColorAcc);
        etWarranty = findViewById(R.id.etWarranty);
        etConditionAcc = findViewById(R.id.etConditionAcc);

        // Stock & Pricing
        etPrice = findViewById(R.id.etPrice);
        btnDecrement = findViewById(R.id.btnDecrement);
        tvQuantityDisplay = findViewById(R.id.tvQuantityDisplay);
        btnIncrement = findViewById(R.id.btnIncrement);
        etLowStockThreshold = findViewById(R.id.etLowStockThreshold);
        cardStockStatus = findViewById(R.id.cardStockStatus);
        tvStockStatusIcon = findViewById(R.id.tvStockStatusIcon);
        tvStockStatusMsg = findViewById(R.id.tvStockStatusMsg);

        // Action Buttons
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);

        // TextWatchers for progress
        TextWatcher progressWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                updateProgressBar();
            }
        };

        etItemName.addTextChangedListener(progressWatcher);
        etBrand.addTextChangedListener(progressWatcher);
        etPrice.addTextChangedListener(progressWatcher);

        btnSave.setOnClickListener(v -> saveItem());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void updateProgressBar() {
        int filled = 0;
        if (!TextUtils.isEmpty(getText(etItemName))) filled++;
        if (!TextUtils.isEmpty(getText(etBrand))) filled++;
        if (getSelectedCategory() != null) filled++;
        if (!TextUtils.isEmpty(getText(etPrice))) filled++;

        int percent = (filled * 100) / 4;
        progressStep.setProgress(percent);
        tvStepPercent.setText(percent + "%");

        if (percent <= 25) {
            tvStepLabel.setText(isEditMode ? "Item Details — Incomplete" : "Step 1 of 2 — Basic Info");
        } else if (percent <= 75) {
            tvStepLabel.setText(isEditMode ? "Item Details — Progress" : "Step 2 of 2 — Stock & Pricing");
        } else {
            tvStepLabel.setText("Almost done!");
        }
    }

    private void setupBrandChips() {
        for (int i = 0; i < brandChipGroup.getChildCount(); i++) {
            View view = brandChipGroup.getChildAt(i);
            if (view instanceof Chip) {
                Chip chip = (Chip) view;
                chip.setOnClickListener(v -> {
                    String brand = chip.getText().toString();
                    etBrand.setText(brand);
                    if (etBrand.getText() != null) {
                        etBrand.setSelection(brand.length());
                    }
                });
            }
        }
    }

    private void setupCategoryChips() {
        categoryChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                cardSpecsSmartphone.setVisibility(View.GONE);
                cardSpecsAccessory.setVisibility(View.GONE);
            } else {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipSmartphones) {
                    cardSpecsSmartphone.setVisibility(View.VISIBLE);
                    cardSpecsAccessory.setVisibility(View.GONE);
                } else if (checkedId == R.id.chipAccessories) {
                    cardSpecsSmartphone.setVisibility(View.GONE);
                    cardSpecsAccessory.setVisibility(View.VISIBLE);
                }
                tvCategoryError.setVisibility(View.GONE);
            }
            updateProgressBar();
        });
    }

    private void setupQuantityStepper() {
        updateQuantityDisplay();
        btnIncrement.setOnClickListener(v -> {
            currentQuantity++;
            updateQuantityDisplay();
            updateStockPreview();
        });
        btnDecrement.setOnClickListener(v -> {
            if (currentQuantity > 0) {
                currentQuantity--;
                updateQuantityDisplay();
                updateStockPreview();
            }
        });
        
        // Manual Quantity Entry
        tvQuantityDisplay.setOnClickListener(v -> showQuantityInputDialog());
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

        etQty.setText(String.valueOf(currentQuantity));
        etQty.setSelection(etQty.getText().length());
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String val = etQty.getText().toString();
            if (!val.isEmpty()) {
                try {
                    currentQuantity = Integer.parseInt(val);
                    updateQuantityDisplay();
                    updateStockPreview();
                } catch (NumberFormatException ignored) {}
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateQuantityDisplay() {
        tvQuantityDisplay.setText(String.valueOf(currentQuantity));
    }

    private void setupStockPreview() {
        etLowStockThreshold.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                updateStockPreview();
            }
        });
    }

    private void updateStockPreview() {
        String threshStr = getText(etLowStockThreshold);
        if (TextUtils.isEmpty(threshStr)) {
            cardStockStatus.setCardBackgroundColor(ContextCompat.getColor(this, R.color.brand_bg_light));
            tvStockStatusIcon.setText("ℹ️");
            tvStockStatusMsg.setText("Enter threshold to preview");
            tvStockStatusMsg.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        } else {
            try {
                int threshold = Integer.parseInt(threshStr);
                if (currentQuantity <= threshold) {
                    cardStockStatus.setCardBackgroundColor(ContextCompat.getColor(this, R.color.stock_alert_bg));
                    tvStockStatusIcon.setText("⚠️");
                    tvStockStatusMsg.setText("Low stock — quantity at or below threshold");
                    tvStockStatusMsg.setTextColor(ContextCompat.getColor(this, R.color.brand_red));
                } else {
                    cardStockStatus.setCardBackgroundColor(ContextCompat.getColor(this, R.color.stock_good_bg));
                    tvStockStatusIcon.setText("✅");
                    tvStockStatusMsg.setText("Stock level looks good");
                    tvStockStatusMsg.setTextColor(ContextCompat.getColor(this, R.color.brand_green));
                }
            } catch (NumberFormatException e) {
                // Ignore invalid input
            }
        }
    }

    private void setupImagePicker() {
        btnPickImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnRemoveImage.setOnClickListener(v -> {
            selectedImageUri = null;
            existingImageUrl = null;
            imgPreview.setImageResource(android.R.drawable.ic_menu_camera);
            imgPreview.setPadding(40, 40, 40, 40);
            imgPreview.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_muted)));
            btnRemoveImage.setVisibility(View.GONE);
            tvUploadStatus.setText("No photo selected");
            tvUploadStatus.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        });
    }

    private String getSelectedCategory() {
        int checkedId = categoryChipGroup.getCheckedChipId();
        if (checkedId == R.id.chipSmartphones) {
            return "Smartphones";
        } else if (checkedId == R.id.chipAccessories) {
            return "Accessories";
        }
        return null;
    }

    private Map<String, String> buildSpecsMap(String category) {
        Map<String, String> specs = new HashMap<>();
        if ("Smartphones".equals(category) || "Iphones & Tablets".equals(category)) {
            putSpec(specs, "display", etDisplay);
            putSpec(specs, "ram", etRam);
            putSpec(specs, "storage", etStorage);
            putSpec(specs, "battery", etBattery);
            putSpec(specs, "processor", etProcessor);
            putSpec(specs, "os", etOs);
            putSpec(specs, "camera", etCamera);
            putSpec(specs, "color", etSpecColorPhone);
            putSpec(specs, "condition", etConditionPhone);
        } else if ("Accessories".equals(category)) {
            putSpec(specs, "type", etType);
            putSpec(specs, "compatible", etCompatible);
            putSpec(specs, "connectivity", etConnectivity);
            putSpec(specs, "wattage", etWattage);
            putSpec(specs, "color", etSpecColorAcc);
            putSpec(specs, "warranty", etWarranty);
            putSpec(specs, "condition", etConditionAcc);
        }
        return specs;
    }

    private void putSpec(Map<String, String> specs, String key, TextInputEditText field) {
        if (field == null) return;
        String val = field.getText() != null ? field.getText().toString().trim() : "";
        if (!val.isEmpty()) {
            specs.put(key, val);
        }
    }

    private void checkEditMode() {
        Intent intent = getIntent();
        itemId = intent.getStringExtra("itemId");
        String mode = intent.getStringExtra("mode");

        if ("edit".equals(mode) && itemId != null) {
            isEditMode = true;
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit Item");
            }
            btnSave.setText("UPDATE ITEM");
            btnDelete.setVisibility(View.VISIBLE);
            loadItemData();
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Add New Item");
            }
            btnSave.setText("ADD ITEM");
        }
    }

    @SuppressWarnings("unchecked")
    private void loadItemData() {
        db.collection("items").document(itemId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot doc = task.getResult();
                if (doc.exists()) {
                    etItemName.setText(doc.getString("name"));
                    etBrand.setText(doc.getString("brand"));
                    etDescription.setText(doc.getString("description"));
                    
                    Double price = doc.getDouble("price");
                    if (price != null) etPrice.setText(String.valueOf(price));
                    
                    Long threshold = doc.getLong("lowStockThreshold");
                    if (threshold != null) etLowStockThreshold.setText(String.valueOf(threshold));
                    
                    Long qty = doc.getLong("quantity");
                    if (qty != null) {
                        currentQuantity = qty.intValue();
                        updateQuantityDisplay();
                    }

                    String category = doc.getString("category");
                    if ("Smartphones".equals(category) || "Iphones & Tablets".equals(category) || "Smartphones & Tablets".equals(category)) {
                        categoryChipGroup.check(R.id.chipSmartphones);
                        cardSpecsSmartphone.setVisibility(View.VISIBLE);
                    } else if ("Accessories".equals(category) || "Accessories & Peripherals".equals(category)) {
                        categoryChipGroup.check(R.id.chipAccessories);
                        cardSpecsAccessory.setVisibility(View.VISIBLE);
                    }

                    Map<String, Object> specs = (Map<String, Object>) doc.get("specs");
                    if (specs != null) {
                        setFieldText(etDisplay, specs, "display");
                        setFieldText(etRam, specs, "ram");
                        setFieldText(etStorage, specs, "storage");
                        setFieldText(etBattery, specs, "battery");
                        setFieldText(etProcessor, specs, "processor");
                        setFieldText(etOs, specs, "os");
                        setFieldText(etCamera, specs, "camera");
                        setFieldText(etSpecColorPhone, specs, "color");
                        setFieldText(etConditionPhone, specs, "condition");
                        
                        setFieldText(etType, specs, "type");
                        setFieldText(etCompatible, specs, "compatible");
                        setFieldText(etConnectivity, specs, "connectivity");
                        setFieldText(etWattage, specs, "wattage");
                        setFieldText(etSpecColorAcc, specs, "color");
                        setFieldText(etWarranty, specs, "warranty");
                        setFieldText(etConditionAcc, specs, "condition");
                    }

                    existingImageUrl = doc.getString("imageUrl");
                    if (!TextUtils.isEmpty(existingImageUrl)) {
                        tvUploadStatus.setText("Photo already saved");
                        tvUploadStatus.setTextColor(ContextCompat.getColor(this, R.color.brand_blue));
                        btnRemoveImage.setVisibility(View.VISIBLE);
                        
                        imgPreview.setPadding(0, 0, 0, 0);
                        imgPreview.setImageTintList(null); // Clear tint for loaded image
                        
                        Glide.with(this)
                                .load(existingImageUrl)
                                .centerCrop()
                                .placeholder(android.R.drawable.ic_menu_camera)
                                .into(imgPreview);
                    }

                    updateStockPreview();
                    updateProgressBar();
                }
            }
        });
    }

    private void setFieldText(TextInputEditText field, Map<String, Object> specs, String key) {
        if (field == null || specs == null) return;
        Object val = specs.get(key);
        if (val != null) {
            field.setText(val.toString());
        }
    }

    private void saveItem() {
        String name = getText(etItemName);
        String brand = getText(etBrand);
        String category = getSelectedCategory();
        String priceStr = getText(etPrice);
        String threshStr = getText(etLowStockThreshold);

        boolean isValid = true;
        if (TextUtils.isEmpty(name)) { etItemName.setError("Required"); isValid = false; }
        if (TextUtils.isEmpty(brand)) { etBrand.setError("Required"); isValid = false; }
        if (category == null) { tvCategoryError.setVisibility(View.VISIBLE); isValid = false; }
        if (TextUtils.isEmpty(priceStr)) { etPrice.setError("Required"); isValid = false; }
        if (TextUtils.isEmpty(threshStr)) { etLowStockThreshold.setError("Required"); isValid = false; }

        if (!isValid) return;

        double price;
        int threshold;
        try {
            price = Double.parseDouble(priceStr);
            threshold = Integer.parseInt(threshStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        if (selectedImageUri != null) {
            uploadToCloudinaryThenSave(name, brand, category, price, threshold);
        } else {
            persistItem(name, brand, category, price, threshold, existingImageUrl);
        }
    }

    private void uploadToCloudinaryThenSave(String name, String brand, String category, double price, int threshold) {
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(selectedImageUri);
                if (is == null) {
                    runOnUiThread(() -> persistItem(name, brand, category, price, threshold, null));
                    return;
                }

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                byte[] imageBytes = buffer.toByteArray();
                is.close();

                OkHttpClient client = new OkHttpClient();
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("upload_preset", CLOUDINARY_UPLOAD_PRESET)
                        .addFormDataPart("file", "item.jpg", RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                        .build();

                Request request = new Request.Builder().url(CLOUDINARY_UPLOAD_URL).post(requestBody).build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(AddEditItemActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                            persistItem(name, brand, category, price, threshold, null);
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        String imageUrl = null;
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                JSONObject json = new JSONObject(response.body().string());
                                imageUrl = json.getString("secure_url");
                            } catch (Exception ignored) {}
                        }
                        String finalImageUrl = imageUrl;
                        runOnUiThread(() -> persistItem(name, brand, category, price, threshold, finalImageUrl));
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> persistItem(name, brand, category, price, threshold, null));
            }
        }).start();
    }

    private void persistItem(String name, String brand, String category, double price, int threshold, String imageUrl) {
        Map<String, Object> itemMap = new HashMap<>();
        String uid = mAuth.getUid();
        if (uid != null) {
            itemMap.put("userId", uid);
        }
        itemMap.put("name", name);
        itemMap.put("brand", brand);
        itemMap.put("category", category);
        itemMap.put("description", getText(etDescription));
        itemMap.put("price", price);
        itemMap.put("quantity", currentQuantity);
        itemMap.put("lowStockThreshold", threshold);
        itemMap.put("specs", buildSpecsMap(category));
        if (imageUrl != null) itemMap.put("imageUrl", imageUrl);

        if (isEditMode) {
            db.collection("items").document(itemId).update(itemMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Item updated!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        resetSaveButton();
                    });
        } else {
            itemMap.put("timestamp", Timestamp.now());
            db.collection("items").add(itemMap)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Item added!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, InventoryActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Add failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        resetSaveButton();
                    });
        }
    }

    private void resetSaveButton() {
        btnSave.setEnabled(true);
        btnSave.setText(isEditMode ? "UPDATE ITEM" : "ADD ITEM");
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Permanently delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("items").document(itemId).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Item deleted.", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
