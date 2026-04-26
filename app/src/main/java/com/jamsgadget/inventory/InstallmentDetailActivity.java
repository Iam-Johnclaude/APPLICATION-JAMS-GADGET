package com.jamsgadget.inventory;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.jamsgadget.inventory.adapter.PaymentHistoryAdapter;
import com.jamsgadget.inventory.util.EmailUtil;
import com.jamsgadget.inventory.util.ThemeHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InstallmentDetailActivity extends AppCompatActivity {

    private TextView tvHeroItemName, tvHeroPaymentType, tvHeroTotalAmount, tvHeroPaidSummary, tvHeroMonthsSummary;
    private ProgressBar heroProgressBar;
    private RecyclerView rvPayments;
    private Button btnRecordPayment, btnSendEmail;
    private LinearLayout layoutSuccessBanner;
    private TextView tvSuccessBanner;

    private FirebaseFirestore db;
    private String installmentId;
    private DocumentSnapshot installmentDoc;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_installment_detail);

        installmentId = getIntent().getStringExtra("installmentId");
        if (installmentId == null) {
            Toast.makeText(this, "Installment not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        loadInstallmentDetails();

        btnRecordPayment.setOnClickListener(v -> openRecordPaymentBottomSheet());
        btnSendEmail.setOnClickListener(v -> sendEmailReminder());
    }

    private void initViews() {
        tvHeroItemName = findViewById(R.id.tvHeroItemName);
        tvHeroPaymentType = findViewById(R.id.tvHeroPaymentType);
        tvHeroTotalAmount = findViewById(R.id.tvHeroTotalAmount);
        tvHeroPaidSummary = findViewById(R.id.tvHeroPaidSummary);
        tvHeroMonthsSummary = findViewById(R.id.tvHeroMonthsSummary);
        heroProgressBar = findViewById(R.id.heroProgressBar);
        rvPayments = findViewById(R.id.rvPayments);
        btnRecordPayment = findViewById(R.id.btnRecordPayment);
        btnSendEmail = findViewById(R.id.btnSendEmail);
        layoutSuccessBanner = findViewById(R.id.layoutSuccessBanner);
        tvSuccessBanner = findViewById(R.id.tvSuccessBanner);

        rvPayments.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadInstallmentDetails() {
        db.collection("installments").document(installmentId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (doc == null || !doc.exists()) {
                        Toast.makeText(this, "Installment no longer exists", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    installmentDoc = doc;
                    updateUI(doc);
                });
    }

    @SuppressWarnings("unchecked")
    private void updateUI(DocumentSnapshot doc) {
        String name = doc.getString("customerName");
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(name != null ? name : "Details");

        String itemName = doc.getString("itemName");
        String paymentType = doc.getString("paymentType");
        double itemPrice = getDouble(doc, "itemPrice");
        double totalPaid = getDouble(doc, "totalPaid");
        int monthsPaid = getInt(doc, "monthsPaid");
        int totalMonths = getInt(doc, "totalMonths");
        String status = doc.getString("status");

        // Use Document ID as fallback if customerId/installmentId is missing
        String custId = doc.getString("customerId");
        if (custId == null || custId.isEmpty()) custId = doc.getId().substring(0, 6).toUpperCase();
        
        String instId = doc.getString("installmentId");
        if (instId == null || instId.isEmpty()) instId = doc.getId().substring(0, 6).toUpperCase();

        tvHeroItemName.setText(itemName != null ? itemName : "—");
        tvHeroPaymentType.setText(paymentType != null ? paymentType : "—");
        tvHeroTotalAmount.setText(String.format(Locale.getDefault(), "₱%,.2f", itemPrice));
        tvHeroPaidSummary.setText(String.format(Locale.getDefault(), "Paid ₱%,.2f", totalPaid));
        tvHeroMonthsSummary.setText(String.format(Locale.getDefault(), "%d of %d months", monthsPaid, totalMonths));

        if (itemPrice > 0) {
            int progress = (int) ((totalPaid / itemPrice) * 100);
            heroProgressBar.setProgress(Math.min(100, progress));
        } else {
            heroProgressBar.setProgress(0);
        }

        // Apply brand colors to progress bar
        int progressColor = "Overdue".equalsIgnoreCase(status) ? R.color.brand_red : R.color.brand_green;
        heroProgressBar.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(this, progressColor)));

        // Row details
        setRowData(R.id.rowCustomerId, "Customer ID", custId);
        setRowData(R.id.rowInstallmentId, "Installment ID", instId);
        setRowData(R.id.rowCustomerName, "Customer Name", name);
        setRowData(R.id.rowEmail, "Email Address", doc.getString("email"));
        setRowData(R.id.rowContact, "Contact Number", doc.getString("contactNumber"));
        setRowData(R.id.rowAddress, "Address", doc.getString("address"));
        setRowData(R.id.rowItem, "Item", itemName);
        setRowData(R.id.rowDownPayment, "Down Payment", String.format(Locale.getDefault(), "₱%,.2f", getDouble(doc, "downPayment")));
        setRowData(R.id.rowBalance, "Balance", String.format(Locale.getDefault(), "₱%,.2f", getDouble(doc, "balance")));
        setRowData(R.id.rowMonthly, "Monthly Payment", String.format(Locale.getDefault(), "₱%,.2f", getDouble(doc, "monthlyPayment")));
        
        // Populate Installment Term
        setRowData(R.id.rowTerm, "Installment Term", String.valueOf(totalMonths));

        Timestamp startTs = doc.getTimestamp("startDate");
        setRowData(R.id.rowStartDate, "Start Date", formatDate(startTs));
        
        // Calculate End Date: Start Date + totalMonths
        if (startTs != null && totalMonths > 0) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(startTs.toDate());
            cal.add(Calendar.MONTH, totalMonths);
            setRowData(R.id.rowEndDate, "End Date", dateFormat.format(cal.getTime()));
        } else {
            setRowData(R.id.rowEndDate, "End Date", "—");
        }

        setRowData(R.id.rowNextDue, "Next Due Date", formatDate(doc.getTimestamp("nextDueDate")));
        setRowData(R.id.rowStatus, "Status", status);

        // Status color for the row using brand colors
        View rowStatus = findViewById(R.id.rowStatus);
        if (rowStatus != null) {
            TextView tvStatusValue = rowStatus.findViewById(R.id.tvValue);
            if (tvStatusValue != null) {
                int statusTextColor;
                if ("Active".equalsIgnoreCase(status)) statusTextColor = R.color.brand_green;
                else if ("Overdue".equalsIgnoreCase(status)) statusTextColor = R.color.brand_red;
                else if ("Paid".equalsIgnoreCase(status)) statusTextColor = R.color.brand_blue;
                else statusTextColor = R.color.text_muted;
                
                tvStatusValue.setTextColor(ContextCompat.getColor(this, statusTextColor));
            }
        }

        // History
        List<Map<String, Object>> payments = (List<Map<String, Object>>) doc.get("payments");
        if (payments != null) {
            rvPayments.setAdapter(new PaymentHistoryAdapter(payments));
        }

        boolean isPaid = "Paid".equalsIgnoreCase(status);
        btnRecordPayment.setVisibility(isPaid ? View.GONE : View.VISIBLE);
        btnSendEmail.setVisibility(isPaid ? View.GONE : View.VISIBLE);
    }

    private void setRowData(int viewId, String label, String value) {
        View row = findViewById(viewId);
        if (row != null) {
            TextView tvLabel = row.findViewById(R.id.tvLabel);
            TextView tvValue = row.findViewById(R.id.tvValue);
            if (tvLabel != null) tvLabel.setText(label);
            if (tvValue != null) tvValue.setText(value != null && !value.isEmpty() ? value : "—");
        }
    }

    private String formatDate(Timestamp ts) {
        return ts != null ? dateFormat.format(ts.toDate()) : "—";
    }

    private double getDouble(DocumentSnapshot doc, String field) {
        Double val = doc.getDouble(field);
        return val != null ? val : 0;
    }

    private int getInt(DocumentSnapshot doc, String field) {
        Long val = doc.getLong(field);
        return val != null ? val.intValue() : 0;
    }

    private void sendEmailReminder() {
        if (installmentDoc == null) return;
        
        String email = installmentDoc.getString("email");
        String name = installmentDoc.getString("customerName");
        String itemName = installmentDoc.getString("itemName");
        double monthly = getDouble(installmentDoc, "monthlyPayment");
        Timestamp nextDue = installmentDoc.getTimestamp("nextDueDate");
        
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Customer has no email address set.", Toast.LENGTH_SHORT).show();
            return;
        }

        EmailUtil.sendDueDateReminder(this, email, name, itemName, monthly, formatDate(nextDue));
    }

    @SuppressWarnings("unchecked")
    private void openRecordPaymentBottomSheet() {
        if (installmentDoc == null) return;
        List<Map<String, Object>> payments = (List<Map<String, Object>>) installmentDoc.get("payments");
        if (payments == null) return;

        Map<String, Object> nextPending = null;
        for (Map<String, Object> p : payments) {
            Object isPaid = p.get("isPaid");
            if (isPaid instanceof Boolean && !(Boolean) isPaid) {
                nextPending = p;
                break;
            }
        }

        if (nextPending == null) {
            Toast.makeText(this, "All payments have been recorded!", Toast.LENGTH_SHORT).show();
            return;
        }

        long monthNum = getLongFromMap(nextPending, "monthNumber");
        double amount = getDoubleFromMap(nextPending, "amount");
        Timestamp dueDate = (Timestamp) nextPending.get("dueDate");
        
        double balance = getDouble(installmentDoc, "balance");
        int totalMonths = getInt(installmentDoc, "totalMonths");
        int monthsPaid = getInt(installmentDoc, "monthsPaid");
        int monthsLeft = totalMonths - monthsPaid;

        RecordPaymentBottomSheet sheet = RecordPaymentBottomSheet.newInstance(
                installmentId, String.valueOf(monthNum), (int) monthNum, amount, balance, monthsLeft, dueDate,
                (pid, method, date) -> {
                    showSuccessBanner((int) monthNum, amount, method);
                });
        sheet.show(getSupportFragmentManager(), "RecordPayment");
    }

    private long getLongFromMap(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        return 0;
    }

    private double getDoubleFromMap(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0;
    }

    private void showSuccessBanner(int month, double amount, String method) {
        layoutSuccessBanner.setVisibility(View.VISIBLE);
        tvSuccessBanner.setText(String.format(Locale.getDefault(), "✅ Month %d · ₱%,.2f via %s recorded!", month, amount, method));
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) layoutSuccessBanner.setVisibility(View.GONE);
        }, 4000);
    }
}
