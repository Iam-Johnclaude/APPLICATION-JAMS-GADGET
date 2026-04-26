package com.jamsgadget.inventory.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.jamsgadget.inventory.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PaymentHistoryAdapter extends RecyclerView.Adapter<PaymentHistoryAdapter.ViewHolder> {

    private final List<Map<String, Object>> payments;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public PaymentHistoryAdapter(List<Map<String, Object>> payments) {
        this.payments = payments;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> payment = payments.get(position);
        if (payment == null) return;

        long monthNum = getLong(payment.get("monthNumber"));
        Timestamp dueDateTs = (Timestamp) payment.get("dueDate");
        boolean isPaid = getBoolean(payment.get("isPaid"));
        double amount = getDouble(payment.get("amount"));
        String method = (String) payment.get("paymentMethod");

        holder.tvMonthLabel.setText("Month " + monthNum);

        if (isPaid && method != null) {
            holder.tvDueDate.setText(method + " • " + formatDate(dueDateTs));
            holder.tvDueDate.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            holder.tvDueDate.setText("Due: " + formatDate(dueDateTs));
            holder.tvDueDate.setTextColor(Color.parseColor("#888888"));
        }

        String amountStr = String.format(Locale.getDefault(), "₱%,.2f", amount);
        
        if (isPaid) {
            holder.tvPaymentStatus.setText(amountStr + " ✓ Paid");
            holder.tvPaymentStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            holder.tvPaymentStatus.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar dueDate = Calendar.getInstance();
            if (dueDateTs != null) {
                dueDate.setTime(dueDateTs.toDate());
                dueDate.set(Calendar.HOUR_OF_DAY, 0);
                dueDate.set(Calendar.MINUTE, 0);
                dueDate.set(Calendar.SECOND, 0);
                dueDate.set(Calendar.MILLISECOND, 0);
            }

            if (dueDateTs != null && dueDate.before(today)) {
                holder.tvPaymentStatus.setText(amountStr + " Overdue");
                holder.tvPaymentStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
                holder.tvPaymentStatus.setTextColor(Color.parseColor("#C62828"));
            } else if (dueDateTs != null && dueDate.equals(today)) {
                holder.tvPaymentStatus.setText(amountStr + " Due Today");
                holder.tvPaymentStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
                holder.tvPaymentStatus.setTextColor(Color.parseColor("#1565C0"));
            } else {
                holder.tvPaymentStatus.setText(amountStr + " Pending");
                holder.tvPaymentStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF3E0")));
                holder.tvPaymentStatus.setTextColor(Color.parseColor("#E65100"));
            }
        }
    }

    private String formatDate(Timestamp ts) {
        return ts != null ? dateFormat.format(ts.toDate()) : "—";
    }

    private double getDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        return 0;
    }

    private long getLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        return 0;
    }

    private boolean getBoolean(Object o) {
        if (o instanceof Boolean) return (Boolean) o;
        return false;
    }

    @Override
    public int getItemCount() {
        return payments != null ? payments.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonthLabel, tvDueDate, tvPaymentStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvMonthLabel = itemView.findViewById(R.id.tvMonthLabel);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            tvPaymentStatus = itemView.findViewById(R.id.tvPaymentStatus);
        }
    }
}
