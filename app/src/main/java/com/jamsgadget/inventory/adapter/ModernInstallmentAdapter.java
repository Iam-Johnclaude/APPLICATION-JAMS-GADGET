package com.jamsgadget.inventory.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.jamsgadget.inventory.R;
import java.util.List;
import java.util.Locale;

public class ModernInstallmentAdapter extends RecyclerView.Adapter<ModernInstallmentAdapter.ViewHolder> {

    private List<InstallmentItem> items;

    public ModernInstallmentAdapter(List<InstallmentItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_installment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InstallmentItem item = items.get(position);
        holder.tvCustomerName.setText(item.customerName);
        holder.tvProductName.setText(item.itemName);
        holder.tvMonthlyAmount.setText(String.format(Locale.getDefault(), "₱%,.2f / mo", item.monthlyAmount));
        holder.tvPaidAmount.setText(String.format(Locale.getDefault(), "Paid: ₱%,.2f", item.totalAmount - item.remainingAmount));
        holder.tvTotalAmount.setText(String.format(Locale.getDefault(), "Total: ₱%,.2f", item.totalAmount));
        
        int progress = (int) ((item.totalAmount - item.remainingAmount) / item.totalAmount * 100);
        holder.pbPayment.setProgress(progress);

        if (progress >= 100) {
            holder.tvStatus.setText("PAID");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.status_paid));
            holder.cardStatusBadge.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.bg_paid_light)));
            holder.pbPayment.setProgressTintList(android.content.res.ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.status_paid)));
        } else {
            holder.tvStatus.setText("PENDING");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.status_pending));
            holder.cardStatusBadge.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.bg_pending_light)));
            holder.pbPayment.setProgressTintList(android.content.res.ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.colorPrimary)));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvProductName, tvStatus, tvMonthlyAmount, tvPaidAmount, tvTotalAmount;
        ProgressBar pbPayment;
        MaterialCardView cardStatusBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            cardStatusBadge = itemView.findViewById(R.id.cardStatusBadge);
            tvMonthlyAmount = itemView.findViewById(R.id.tvMonthlyAmount);
            tvPaidAmount = itemView.findViewById(R.id.tvPaidAmount);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
            pbPayment = itemView.findViewById(R.id.pbPayment);
        }
    }

    public static class InstallmentItem {
        public String customerName;
        public String itemName;
        public double monthlyAmount;
        public double remainingAmount;
        public double totalAmount;

        public InstallmentItem(String customerName, String itemName, double monthlyAmount, double remainingAmount, double totalAmount) {
            this.customerName = customerName;
            this.itemName = itemName;
            this.monthlyAmount = monthlyAmount;
            this.remainingAmount = remainingAmount;
            this.totalAmount = totalAmount;
        }
    }
}
