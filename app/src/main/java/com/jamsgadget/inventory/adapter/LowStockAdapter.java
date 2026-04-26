package com.jamsgadget.inventory.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jamsgadget.inventory.R;
import com.jamsgadget.inventory.model.Item;
import java.util.List;

public class LowStockAdapter extends RecyclerView.Adapter<LowStockAdapter.ViewHolder> {
    private List<Item> items;

    public LowStockAdapter(List<Item> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_low_stock, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        holder.tvItemName.setText(item.getName());
        holder.tvItemCategory.setText(item.getCategory() + " • " + item.getBrand());
        holder.tvItemQty.setText("Qty: " + item.getQuantity());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateList(List<Item> newList) {
        this.items = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvItemCategory, tvItemQty;

        ViewHolder(View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemCategory = itemView.findViewById(R.id.tvItemCategory);
            tvItemQty = itemView.findViewById(R.id.tvItemQty);
        }
    }
}