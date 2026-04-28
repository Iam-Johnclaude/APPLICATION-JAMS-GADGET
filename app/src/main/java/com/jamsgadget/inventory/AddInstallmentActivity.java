package com.jamsgadget.inventory;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.jamsgadget.inventory.adapter.ItemPickerAdapter;
import com.jamsgadget.inventory.util.ThemeHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddInstallmentActivity extends AppCompatActivity {

    private static final String TAG = "AddInstallmentActivity";
    private TextInputEditText etCustomerName, etEmail, etContactNumber, etAddress;
    private TextInputEditText etItemPrice, etDownPayment, etStartDate;
    private AutoCompleteTextView actvMonths;
    private TextView tvMonthlyPayment, tvSummaryBalance, tvSummaryMonthly,
            tvSummaryDueDay, tvSummaryTotalPayments;
    private LinearLayout layoutItemSelector;
    private Button btnSave;

    private ImageView imgSelectedItem;
    private TextView tvSelectedItemIcon, tvSelectedItemName,
            tvSelectedItemBrand, tvSelectedItemCategory,
            tvSelectedItemPrice;

    private FirebaseFirestore db;
    private String selectedItemId = null;
    private String selectedItemName = null;
    private double selectedItemPrice = 0;
    private Calendar startDateCalendar = null;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    private ItemPickerAdapter itemPickerAdapter;
    private final List<DocumentSnapshot> allInventoryItems = new ArrayList<>();
    private Dialog itemPickerDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_installment);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupMonthsDropdown();
        setupLiveCompute();
        setupItemSelector();
        setupDatePicker();

        btnSave.setOnClickListener(v -> saveInstallment());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        etCustomerName = findViewById(R.id.etCustomerName);
        etEmail = findViewById(R.id.etEmail);
        etContactNumber = findViewById(R.id.etContactNumber);
        etAddress = findViewById(R.id.etAddress);
        etItemPrice = findViewById(R.id.etItemPrice);
        etDownPayment = findViewById(R.id.etDownPayment);
        etStartDate = findViewById(R.id.etStartDate);
        actvMonths = findViewById(R.id.actvMonths);
        tvMonthlyPayment = findViewById(R.id.tvMonthlyPayment);
        tvSummaryBalance = findViewById(R.id.tvSummaryBalance);
        tvSummaryMonthly = findViewById(R.id.tvSummaryMonthly);
        tvSummaryDueDay = findViewById(R.id.tvSummaryDueDay);
        tvSummaryTotalPayments = findViewById(R.id.tvSummaryTotalPayments);
        layoutItemSelector = findViewById(R.id.layoutItemSelector);
        btnSave = findViewById(R.id.btnSave);

        imgSelectedItem = findViewById(R.id.imgSelectedItem);
        tvSelectedItemIcon = findViewById(R.id.tvSelectedItemIcon);
        tvSelectedItemName = findViewById(R.id.tvSelectedItemName);
        tvSelectedItemBrand = findViewById(R.id.tvSelectedItemBrand);
        tvSelectedItemCategory = findViewById(R.id.tvSelectedItemCategory);
        tvSelectedItemPrice = findViewById(R.id.tvSelectedItemPrice);
    }

    private void setupMonthsDropdown() {
        String[] options = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, options);
        actvMonths.setAdapter(adapter);
    }

    private void setupLiveCompute() {
        TextWatcher computeWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                recomputeSummary();
            }
        };

        etItemPrice.addTextChangedListener(computeWatcher);
        etDownPayment.addTextChangedListener(computeWatcher);
        actvMonths.setOnItemClickListener((parent, view, position, id) -> recomputeSummary());
    }

    private void recomputeSummary() {
        String priceStr = etItemPrice.getText() != null ? etItemPrice.getText().toString() : "";
        String downStr = etDownPayment.getText() != null ? etDownPayment.getText().toString() : "";
        String monthsStr = actvMonths.getText() != null ? actvMonths.getText().toString() : "";

        double price = parseDouble(priceStr);
        double down = parseDouble(downStr);
        int months = parseInt(monthsStr);

        double balance = price - down;
        double monthly = months > 0 ? balance / months : 0;

        tvMonthlyPayment.setText(String.format(Locale.getDefault(), "₱%,.2f", monthly));
        tvSummaryBalance.setText(String.format(Locale.getDefault(), "₱%,.2f", balance));
        tvSummaryMonthly.setText(String.format(Locale.getDefault(), "₱%,.2f", monthly));
        tvSummaryTotalPayments.setText(String.valueOf(months));

        if (startDateCalendar != null) {
            int dueDay = startDateCalendar.get(Calendar.DAY_OF_MONTH);
            String dueDayText = dueDay + getDaySuffix(dueDay);
            tvSummaryDueDay.setText(dueDayText);
        } else {
            tvSummaryDueDay.setText("—");
        }
    }

    private String getDaySuffix(int n) {
        if (n >= 11 && n <= 13) return "th";
        switch (n % 10) {
            case 1:  return "st";
            case 2:  return "nd";
            case 3:  return "rd";
            default: return "th";
        }
    }

    private void setupItemSelector() {
        layoutItemSelector.setOnClickListener(v -> showItemPickerDialog());
    }

    private void showItemPickerDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_item_picker, null);
        itemPickerDialog = new Dialog(this, R.style.Theme_JamsGadget_Dialog);
        itemPickerDialog.setContentView(dialogView);
        if (itemPickerDialog.getWindow() != null) {
            itemPickerDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            itemPickerDialog.getWindow().setGravity(Gravity.BOTTOM);
            itemPickerDialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_InputMethod;
        }
        itemPickerDialog.show();

        TextInputEditText etSearch = dialogView.findViewById(R.id.etItemPickerSearch);
        ChipGroup chipGroup = dialogView.findViewById(R.id.chipGroupItemFilter);
        RecyclerView rvItems = dialogView.findViewById(R.id.rvItemPicker);
        TextView tvCount = dialogView.findViewById(R.id.tvPickerItemCount);
        LinearLayout layoutEmpty = dialogView.findViewById(R.id.layoutPickerEmpty);
        dialogView.findViewById(R.id.tvDialogClose).setOnClickListener(v -> itemPickerDialog.dismiss());

        itemPickerAdapter = new ItemPickerAdapter(allInventoryItems, doc -> {
            selectedItemId = doc.getId();
            selectedItemName = doc.getString("name") != null ? doc.getString("name") : "";
            Double priceVal = doc.getDouble("price");
            selectedItemPrice = priceVal != null ? priceVal : 0;
            String brand = doc.getString("brand") != null ? doc.getString("brand") : "";
            String category = doc.getString("category") != null ? doc.getString("category") : "";
            String imageUrl = doc.getString("imageUrl");

            // Normalize Category Display
            String displayCategory = category;
            if (category.toLowerCase().contains("iphone") || category.toLowerCase().contains("phone") || category.toLowerCase().contains("tablet")) {
                displayCategory = "Smartphones";
            } else if (category.toLowerCase().contains("acc")) {
                displayCategory = "Accessories";
            }

            tvSelectedItemName.setText(selectedItemName);
            tvSelectedItemBrand.setText(brand);
            tvSelectedItemCategory.setText(displayCategory);
            tvSelectedItemPrice.setText(String.format(Locale.getDefault(), "₱%,.2f", selectedItemPrice));

            if (imageUrl != null && !imageUrl.isEmpty()) {
                imgSelectedItem.setVisibility(View.VISIBLE);
                tvSelectedItemIcon.setVisibility(View.GONE);
                Glide.with(this).load(imageUrl)
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(16)))
                        .into(imgSelectedItem);
            } else {
                imgSelectedItem.setVisibility(View.GONE);
                tvSelectedItemIcon.setVisibility(View.VISIBLE);
                tvSelectedItemIcon.setText(displayCategory.equals("Smartphones") ? "📱" : "🎧");
            }

            etItemPrice.setText(String.valueOf(selectedItemPrice));
            recomputeSummary();
            itemPickerDialog.dismiss();
        });

        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(itemPickerAdapter);

        Runnable updateCount = () -> {
            int count = itemPickerAdapter.getFilteredCount();
            String countText = count + " item" + (count != 1 ? "s" : "");
            tvCount.setText(countText);
            boolean isEmpty = count == 0;
            rvItems.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        };

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            String category = "All";
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipItemSmartphones) category = "Smartphones";
                else if (checkedId == R.id.chipItemAccessories) category = "Accessories";
            }
            itemPickerAdapter.filter(etSearch.getText().toString().trim(), category);
            updateCount.run();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String category = "All";
                int checkedId = chipGroup.getCheckedChipId();
                if (checkedId == R.id.chipItemSmartphones) category = "Smartphones";
                else if (checkedId == R.id.chipItemAccessories) category = "Accessories";
                
                itemPickerAdapter.filter(s.toString().trim(), category);
                updateCount.run();
            }
        });

        if (allInventoryItems.isEmpty()) {
            db.collection("items").orderBy("name").get().addOnSuccessListener(snapshots -> {
                allInventoryItems.clear();
                allInventoryItems.addAll(snapshots.getDocuments());
                itemPickerAdapter.updateList(allInventoryItems);
                updateCount.run();
            });
        } else {
            itemPickerAdapter.updateList(allInventoryItems);
            updateCount.run();
        }
    }

    private void setupDatePicker() {
        etStartDate.setOnClickListener(v -> {
            Calendar c = startDateCalendar != null ? startDateCalendar : Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                startDateCalendar = Calendar.getInstance();
                startDateCalendar.set(year, month, dayOfMonth);
                etStartDate.setText(dateFormat.format(startDateCalendar.getTime()));
                recomputeSummary();
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void saveInstallment() {
        String customerName = etCustomerName.getText() != null ? etCustomerName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String contact = etContactNumber.getText() != null ? etContactNumber.getText().toString().trim() : "";
        String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        
        String priceStr = etItemPrice.getText() != null ? etItemPrice.getText().toString() : "";
        String downStr = etDownPayment.getText() != null ? etDownPayment.getText().toString() : "";
        String monthsStr = actvMonths.getText() != null ? actvMonths.getText().toString() : "";

        double price = parseDouble(priceStr);
        double down = parseDouble(downStr);
        int months = parseInt(monthsStr);

        if (TextUtils.isEmpty(customerName)) { etCustomerName.setError("Required"); return; }
        if (selectedItemId == null) { Toast.makeText(this, "Please select an item", Toast.LENGTH_SHORT).show(); return; }
        if (price <= 0) { etItemPrice.setError("Invalid price"); return; }
        if (months <= 0) { Toast.makeText(this, "Please select duration", Toast.LENGTH_SHORT).show(); return; }
        if (startDateCalendar == null) { Toast.makeText(this, "Please select start date", Toast.LENGTH_SHORT).show(); return; }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        db.collection("installments").get().addOnSuccessListener(queryDocumentSnapshots -> {
            int nextIdNum = queryDocumentSnapshots.size() + 1;
            String nextId = String.valueOf(nextIdNum);

            double balance = price - down;
            double monthly = balance / months;

            Calendar firstDueDate = (Calendar) startDateCalendar.clone();
            firstDueDate.add(Calendar.MONTH, 1);

            List<Map<String, Object>> payments = new ArrayList<>();
            for (int i = 1; i <= months; i++) {
                Calendar dueDate = (Calendar) startDateCalendar.clone();
                dueDate.add(Calendar.MONTH, i); 
                Map<String, Object> payment = new HashMap<>();
                payment.put("monthNumber", i);
                payment.put("dueDate", new Timestamp(dueDate.getTime()));
                payment.put("amount", monthly);
                payment.put("isPaid", false);
                payments.add(payment);
            }

            Map<String, Object> map = new HashMap<>();
            map.put("customerId", nextId);
            map.put("installmentId", nextId);
            map.put("customerName", customerName);
            map.put("email", email);
            map.put("contactNumber", contact);
            map.put("address", address);
            map.put("itemId", selectedItemId);
            map.put("itemName", selectedItemName);
            map.put("itemPrice", price);
            map.put("downPayment", down);
            map.put("balance", balance);
            map.put("totalMonths", months);
            map.put("monthlyPayment", monthly);
            map.put("startDate", new Timestamp(startDateCalendar.getTime()));
            map.put("nextDueDate", new Timestamp(firstDueDate.getTime())); 
            map.put("paymentType", "Home Credit");
            map.put("status", "Active");
            map.put("totalPaid", down);
            map.put("monthsPaid", 0);
            map.put("createdAt", Timestamp.now());
            map.put("payments", payments);

            db.collection("installments").add(map)
                .addOnSuccessListener(documentReference -> {
                    syncWithGoogleCalendar(payments, customerName, selectedItemName);
                    Toast.makeText(this, "Installment #" + nextId + " saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("SAVE INSTALLMENT");
                });
        });
    }

    private void syncWithGoogleCalendar(List<Map<String, Object>> payments, String customerName, String itemName) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) {
            Log.w(TAG, "No Google account for calendar sync");
            return;
        }

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                            AddInstallmentActivity.this, 
                            Collections.singleton("https://www.googleapis.com/auth/calendar"));
                    credential.setSelectedAccountName(account.getEmail());

                    com.google.api.services.calendar.Calendar service = new com.google.api.services.calendar.Calendar.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            new GsonFactory(),
                            credential)
                            .setApplicationName("JamsGadget Inventory")
                            .build();

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    for (Map<String, Object> payment : payments) {
                        Timestamp dueDateTs = (Timestamp) payment.get("dueDate");
                        if (dueDateTs == null) continue;

                        // Create All-Day Event
                        String dateString = sdf.format(dueDateTs.toDate());
                        DateTime date = new DateTime(dateString);

                        Event event = new Event()
                                .setSummary("Installment Due: " + customerName + " - " + itemName)
                                .setDescription("Monthly installment payment")
                                .setColorId("11"); // Tomato/Red color in Google Calendar

                        event.setStart(new EventDateTime().setDate(date));
                        event.setEnd(new EventDateTime().setDate(date));

                        service.events().insert("primary", event).execute();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Calendar sync failed", e);
                }
                return null;
            }
        }.execute();
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}
