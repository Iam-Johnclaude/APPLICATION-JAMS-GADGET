package com.jamsgadget.inventory.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jamsgadget.inventory.R;
import com.jamsgadget.inventory.model.Item;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TopItemsAdapter extends RecyclerView.Adapter<TopItemsAdapter.ViewHolder> {
    private List<Item> items = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_top_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        holder.tvRank.setText(String.valueOf(position + 1));
        holder.tvName.setText(item.getName());
        holder.tvSub.setText(item.getBrand() + " • " + item.getCategory());
        
        double totalValue = item.getPrice() * item.getQuantity();
        holder.tvValue.setText(String.format(Locale.getDefault(), "₱%,.2f", totalValue));
        holder.tvQtyLabel.setText("Qty: " + item.getQuantity());

        // Rank badge color
        GradientDrawable background = (GradientDrawable) holder.tvRank.getBackground();
        int color;
        switch (position) {
            case 0: color = Color.parseColor("#FFD700"); break; // Gold
            case 1: color = Color.parseColor("#A8A9AD"); break; // Silver
            case 2: color = Color.parseColor("#CD7F32"); break; // Bronze
            default: color = Color.parseColor("#1565C0"); break; // Electric Blue
        }
        background.setColor(color);
    }

    @Override
    public int getItemCount() {
        return Math.min(items.size(), 5);
    }

    public void updateList(List<Item> newList) {
        this.items = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvName, tvSub, tvValue, tvQtyLabel;

        ViewHolder(View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRankBadge);
            tvName = itemView.findViewById(R.id.tvTopItemName);
            tvSub = itemView.findViewById(R.id.tvTopItemSub);
            tvValue = itemView.findViewById(R.id.tvTopItemValue);
            tvQtyLabel = itemView.findViewById(R.id.tvTopItemQty);
        }
    }
}