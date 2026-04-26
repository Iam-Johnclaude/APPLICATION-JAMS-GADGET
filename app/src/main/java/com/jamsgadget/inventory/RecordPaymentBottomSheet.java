package com.jamsgadget.inventory;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecordPaymentBottomSheet extends BottomSheetDialogFragment {

    public interface OnPaymentRecordedListener {
        void onPaymentRecorded(String paymentId, String method, Date paidAt);
    }

    private String loanId;
    private String paymentId;
    private int monthNumber;
    private double amount;
    private Timestamp dueDate;
    private String selectedMethod = null;
    private Calendar selectedDate = Calendar.getInstance();
    private OnPaymentRecordedListener listener;
    private FirebaseFirestore db;
    private ListenerRegistration registration;

    private TextView tvPaymentSubtitle, tvPillAmount, tvPillRemaining, tvPillMonthsLeft;
    private TextView tvSelectedDate;
    private MaterialCardView cardGCash, cardCash;
    private View radioGCash, radioCash;
    private MaterialButton btnConfirmPayment;
    private LinearLayout layoutDatePicker;

    private List<MaterialCardView> allCards = new ArrayList<>();
    private List<View> allRadios = new ArrayList<>();

    public static RecordPaymentBottomSheet newInstance(
            String loanId, String paymentId, int monthNumber,
            double amount, double remainingBalance, int monthsLeft,
            Timestamp dueDate, OnPaymentRecordedListener listener) {
        RecordPaymentBottomSheet fragment = new RecordPaymentBottomSheet();
        fragment.loanId = loanId;
        fragment.paymentId = paymentId;
        fragment.monthNumber = monthNumber;
        fragment.amount = amount;
        fragment.dueDate = dueDate;
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_record_payment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        initViews(view);
        setupListeners(view);
        startRealtimeUpdates();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (registration != null) {
            registration.remove();
        }
    }

    private void initViews(View view) {
        tvPaymentSubtitle = view.findViewById(R.id.tvPaymentSubtitle);
        tvPillAmount = view.findViewById(R.id.tvPillAmount);
        tvPillRemaining = view.findViewById(R.id.tvPillRemaining);
        tvPillMonthsLeft = view.findViewById(R.id.tvPillMonthsLeft);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        cardGCash = view.findViewById(R.id.cardGCash);
        cardCash = view.findViewById(R.id.cardCash);
        radioGCash = view.findViewById(R.id.radioGCash);
        radioCash = view.findViewById(R.id.radioCash);
        btnConfirmPayment = view.findViewById(R.id.btnConfirmPayment);
        layoutDatePicker = view.findViewById(R.id.layoutDatePicker);

        allCards.clear();
        allCards.add(cardGCash); allCards.add(cardCash);
        allRadios.clear();
        allRadios.add(radioGCash); allRadios.add(radioCash);
    }

    private void startRealtimeUpdates() {
        registration = db.collection("installments").document(loanId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;
                    updateUI(doc);
                });
    }

    @SuppressWarnings("unchecked")
    private void updateUI(DocumentSnapshot doc) {
        double itemPrice = getDouble(doc, "itemPrice");
        double totalPaidSoFar = getDouble(doc, "totalPaid");
        int totalMonths = getInt(doc, "totalMonths");
        int monthsPaidSoFar = getInt(doc, "monthsPaid");

        List<Map<String, Object>> payments = (List<Map<String, Object>>) doc.get("payments");
        if (payments != null) {
            for (Map<String, Object> p : payments) {
                if (getIntFromMap(p, "monthNumber") == monthNumber) {
                    this.amount = getDoubleFromMap(p, "amount");
                    this.dueDate = (Timestamp) p.get("dueDate");
                    
                    Object isPaid = p.get("isPaid");
                    if (isPaid instanceof Boolean && (boolean) isPaid) {
                        Toast.makeText(getContext(), "Payment for Month " + monthNumber + " is already recorded.", Toast.LENGTH_SHORT).show();
                        dismiss();
                        return;
                    }
                    break;
                }
            }
        }

        double currentRemainingDebt = itemPrice - totalPaidSoFar;
        int currentPendingMonths = totalMonths - monthsPaidSoFar;

        tvPaymentSubtitle.setText("Month " + monthNumber + " · Due " + formatDate(dueDate));
        tvPillAmount.setText("₱" + formatMoney(amount));
        tvPillRemaining.setText("₱" + formatMoney(Math.max(0, currentRemainingDebt)));
        tvPillMonthsLeft.setText(Math.max(0, currentPendingMonths) + " left");
        tvSelectedDate.setText((isToday(selectedDate) ? "Today, " : "") + formatFullDate(selectedDate));

        if (selectedMethod != null) {
            btnConfirmPayment.setText("✓ Confirm — ₱" + formatMoney(amount) + " via " + selectedMethod);
        }
    }

    private void setupListeners(View view) {
        cardGCash.setOnClickListener(v -> selectMethod("GCash", cardGCash, radioGCash));
        cardCash.setOnClickListener(v -> selectMethod("Cash", cardCash, radioCash));

        layoutDatePicker.setOnClickListener(v -> showDatePicker());
        btnConfirmPayment.setOnClickListener(v -> confirmPayment());
        view.findViewById(R.id.btnClosePayment).setOnClickListener(v -> dismiss());
    }

    private void selectMethod(String method, MaterialCardView selectedCard, View selectedRadio) {
        this.selectedMethod = method;
        int navyColor = ContextCompat.getColor(requireContext(), R.color.brand_navy);
        int grayBorder = Color.parseColor("#E2E8F0");
        
        for (int i = 0; i < allCards.size(); i++) {
            allCards.get(i).setStrokeColor(grayBorder);
            allCards.get(i).setStrokeWidth(dpToPx(1));
            allCards.get(i).setCardBackgroundColor(Color.WHITE);
            allRadios.get(i).setBackgroundResource(R.drawable.radio_indicator_unselected);
        }
        
        selectedCard.setStrokeColor(navyColor);
        selectedCard.setStrokeWidth(dpToPx(2));
        selectedCard.setCardBackgroundColor(Color.WHITE);
        selectedRadio.setBackgroundResource(R.drawable.radio_indicator_selected);

        btnConfirmPayment.setEnabled(true);
        btnConfirmPayment.setBackgroundTintList(ColorStateList.valueOf(navyColor));
        btnConfirmPayment.setText("✓ Confirm — ₱" + formatMoney(amount) + " via " + method);
    }

    private void showDatePicker() {
        Calendar cal = (Calendar) selectedDate.clone();
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            selectedDate.set(year, month, day);
            String label = isToday(selectedDate) ? "Today, " : "";
            tvSelectedDate.setText(label + formatFullDate(selectedDate));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private boolean isToday(Calendar cal) {
        Calendar today = Calendar.getInstance();
        return cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
    }

    private void confirmPayment() {
        if (selectedMethod == null) return;
        Date paidAtDate = selectedDate.getTime();
        btnConfirmPayment.setEnabled(false);
        btnConfirmPayment.setText("Recording...");

        db.collection("installments").document(loanId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> payments = (List<Map<String, Object>>) doc.get("payments");
            if (payments == null) return;

            int updatedIndex = -1;
            for (int i = 0; i < payments.size(); i++) {
                Map<String, Object> p = payments.get(i);
                if (getIntFromMap(p, "monthNumber") == monthNumber) {
                    p.put("isPaid", true);
                    p.put("status", "Paid");
                    p.put("paymentMethod", selectedMethod);
                    p.put("paidDate", new Timestamp(paidAtDate));
                    updatedIndex = i;
                    break;
                }
            }

            if (updatedIndex != -1) {
                int monthsPaid = getInt(doc, "monthsPaid") + 1;
                double totalPaid = getDouble(doc, "totalPaid") + amount;
                int totalMonths = getInt(doc, "totalMonths");
                String status = (monthsPaid >= totalMonths) ? "Paid" : "Active";
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("payments", payments);
                updates.put("monthsPaid", monthsPaid);
                updates.put("totalPaid", totalPaid);
                updates.put("status", status);
                if (monthsPaid < totalMonths) updates.put("nextDueDate", payments.get(updatedIndex + 1).get("dueDate"));

                db.collection("installments").document(loanId).update(updates).addOnSuccessListener(unused -> {
                    if (listener != null) listener.onPaymentRecorded(paymentId, selectedMethod, paidAtDate);
                    dismiss();
                }).addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnConfirmPayment.setEnabled(true);
                    btnConfirmPayment.setText("✓ Confirm — ₱" + formatMoney(amount) + " via " + selectedMethod);
                });
            }
        });
    }

    private String formatMoney(double amount) { return String.format(Locale.getDefault(), "%,.2f", amount); }
    private String formatDate(Timestamp ts) { return ts != null ? new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(ts.toDate()) : ""; }
    private String formatFullDate(Calendar cal) { return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(cal.getTime()); }
    private int dpToPx(int dp) { return (int) (dp * getResources().getDisplayMetrics().density); }

    private double getDouble(DocumentSnapshot doc, String field) {
        Double val = doc.getDouble(field);
        return val != null ? val : 0;
    }

    private int getInt(DocumentSnapshot doc, String field) {
        Long val = doc.getLong(field);
        return val != null ? val.intValue() : 0;
    }

    private int getIntFromMap(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return 0;
    }

    private double getDoubleFromMap(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0;
    }
}
