package com.jamsgadget.inventory.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.jamsgadget.inventory.R;

import java.util.List;
import java.util.Locale;

public class InstallmentsAdapter extends RecyclerView.Adapter<InstallmentsAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(DocumentSnapshot document);
        void onDeleteClick(DocumentSnapshot document);
    }

    private List<DocumentSnapshot> installments;
    private final OnItemClickListener listener;

    public InstallmentsAdapter(List<DocumentSnapshot> installments, OnItemClickListener listener) {
        this.installments = installments;
        this.listener = listener;
    }

    public void updateList(List<DocumentSnapshot> newList) {
        this.installments = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_installment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = installments.get(position);
        if (doc == null) return;
        
        String customerName = doc.getString("customerName");
        String itemName = doc.getString("itemName");
        String status = doc.getString("status");
        String paymentType = doc.getString("paymentType");
        
        double monthly = getDouble(doc, "monthlyPayment");
        double totalPaid = getDouble(doc, "totalPaid");
        double itemPrice = getDouble(doc, "itemPrice");

        holder.tvCustomerName.setText(customerName != null ? customerName : "Unknown Customer");
        holder.tvProductName.setText(itemName != null ? itemName : "No Item");
        holder.tvSource.setText(paymentType != null ? paymentType : "Other");
        holder.tvStatus.setText(status != null ? status : "Active");
        
        holder.tvMonthlyAmount.setText(String.format(Locale.getDefault(), "₱%,.2f / mo", monthly));
        holder.tvPaidAmount.setText(String.format(Locale.getDefault(), "Paid: ₱%,.2f", totalPaid));
        holder.tvTotalAmount.setText(String.format(Locale.getDefault(), "Total: ₱%,.2f", itemPrice));

        // Status Badge Styling
        int badgeColor, textColor;
        if ("Active".equalsIgnoreCase(status)) {
            badgeColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.stock_good_bg);
            textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.brand_green);
        } else if ("Overdue".equalsIgnoreCase(status)) {
            badgeColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.stock_alert_bg);
            textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.brand_red);
        } else if ("Paid".equalsIgnoreCase(status)) {
            badgeColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.icon_blue_bg);
            textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.brand_blue);
        } else {
            badgeColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.border_color);
            textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.text_muted);
        }
        
        holder.cardStatusBadge.setCardBackgroundColor(ColorStateList.valueOf(badgeColor));
        holder.tvStatus.setTextColor(textColor);

        // Progress Bar logic
        if (itemPrice > 0) {
            int progress = (int) ((totalPaid / itemPrice) * 100);
            holder.pbPayment.setProgress(Math.min(100, progress));
            
            int progressColor = "Overdue".equalsIgnoreCase(status) ? 
                    R.color.brand_red : R.color.brand_green;
            holder.pbPayment.setProgressTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(holder.itemView.getContext(), progressColor)));

            if (holder.tvPaidPercent != null) {
                holder.tvPaidPercent.setText(progress + "% paid");
            }
            if (holder.tvRemainingBalance != null) {
                double remaining = Math.max(0, itemPrice - totalPaid);
                holder.tvRemainingBalance.setText(String.format(Locale.getDefault(), "₱%,.2f left", remaining));
            }
        } else {
            holder.pbPayment.setProgress(0);
            if (holder.tvPaidPercent != null) holder.tvPaidPercent.setText("0% paid");
            if (holder.tvRemainingBalance != null) holder.tvRemainingBalance.setText("₱0.00 left");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(doc);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(doc);
        });
    }

    private double getDouble(DocumentSnapshot doc, String field) {
        Double val = doc.getDouble(field);
        return val != null ? val : 0;
    }

    @Override
    public int getItemCount() {
        return installments != null ? installments.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvStatus, tvProductName, tvSource, tvMonthlyAmount, tvPaidAmount, tvTotalAmount, tvPaidPercent, tvRemainingBalance;
        ProgressBar pbPayment;
        MaterialCardView cardStatusBadge;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            cardStatusBadge = itemView.findViewById(R.id.cardStatusBadge);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvSource = itemView.findViewById(R.id.tvSource);
            tvMonthlyAmount = itemView.findViewById(R.id.tvMonthlyAmount);
            tvPaidAmount = itemView.findViewById(R.id.tvPaidAmount);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
            pbPayment = itemView.findViewById(R.id.pbPayment);
            tvPaidPercent = itemView.findViewById(R.id.tvPaidPercent);
            tvRemainingBalance = itemView.findViewById(R.id.tvRemainingBalance);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
