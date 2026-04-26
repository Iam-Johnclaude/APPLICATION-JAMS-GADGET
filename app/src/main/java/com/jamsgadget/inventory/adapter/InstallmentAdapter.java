package com.jamsgadget.inventory.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.jamsgadget.inventory.R;
import com.jamsgadget.inventory.model.Installment;

import java.util.List;
import java.util.Locale;

public class InstallmentAdapter extends RecyclerView.Adapter<InstallmentAdapter.ViewHolder> {

    private List<Installment> installments;
    private Context context;

    public InstallmentAdapter(List<Installment> installments) {
        this.installments = installments;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_installment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Installment installment = installments.get(position);

        holder.tvCustomerName.setText(installment.getCustomerName());
        holder.tvProductName.setText(installment.getProductName());
        holder.tvSource.setText(installment.getFinancingSource());
        holder.tvStatus.setText(installment.getStatus());

        // CRITICAL RULE: Set / mo ONLY here
        holder.tvMonthlyAmount.setText(String.format(Locale.getDefault(), "₱%,.2f / mo", installment.getMonthlyAmount()));
        
        holder.tvTotalAmount.setText(String.format(Locale.getDefault(), "₱%,.2f", installment.getTotalAmount()));
        holder.tvPaidAmount.setText(String.format(Locale.getDefault(), "Paid: ₱%,.2f", installment.getPaidAmount()));

        int progress = (int) ((installment.getPaidAmount() / installment.getTotalAmount()) * 100);
        holder.pbPayment.setProgress(progress);
        holder.tvPaidPercent.setText(progress + "% paid");

        if (progress >= 100) {
            holder.tvRemainingBalance.setText("Completed");
        } else {
            double remaining = installment.getTotalAmount() - installment.getPaidAmount();
            holder.tvRemainingBalance.setText(String.format(Locale.getDefault(), "₱%,.2f left", remaining));
        }

        // Status specific styling
        int colorRes;
        int badgeBgRes;

        switch (installment.getStatus()) {
            case "Overdue":
                colorRes = R.color.status_overdue;
                badgeBgRes = R.color.status_overdue_light;
                break;
            case "Paid":
                colorRes = R.color.status_paid;
                badgeBgRes = R.color.status_paid_light;
                break;
            case "Active":
            default:
                colorRes = R.color.status_active;
                badgeBgRes = R.color.status_active_light;
                break;
        }

        int color = ContextCompat.getColor(context, colorRes);
        int bgColor = ContextCompat.getColor(context, badgeBgRes);
        
        holder.viewAccent.setBackgroundColor(color);
        holder.tvStatus.setTextColor(color);
        holder.cardStatusBadge.setCardBackgroundColor(ColorStateList.valueOf(bgColor));
        holder.tvPaidAmount.setTextColor(color);
        holder.pbPayment.setProgressTintList(ColorStateList.valueOf(color));
    }

    @Override
    public int getItemCount() {
        return installments.size();
    }

    public void updateList(List<Installment> newList) {
        this.installments = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View viewAccent;
        TextView tvCustomerName, tvStatus, tvProductName, tvSource;
        TextView tvMonthlyAmount, tvTotalAmount, tvPaidAmount, tvPaidPercent, tvRemainingBalance;
        ProgressBar pbPayment;
        MaterialCardView cardStatusBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            viewAccent = itemView.findViewById(R.id.viewAccent);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            cardStatusBadge = itemView.findViewById(R.id.cardStatusBadge);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvSource = itemView.findViewById(R.id.tvSource);
            tvMonthlyAmount = itemView.findViewById(R.id.tvMonthlyAmount);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
            tvPaidAmount = itemView.findViewById(R.id.tvPaidAmount);
            tvPaidPercent = itemView.findViewById(R.id.tvPaidPercent);
            tvRemainingBalance = itemView.findViewById(R.id.tvRemainingBalance);
            pbPayment = itemView.findViewById(R.id.pbPayment);
        }
    }
}