package com.jamsgadget.inventory.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.jamsgadget.inventory.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemPickerAdapter extends RecyclerView.Adapter<ItemPickerAdapter.ViewHolder> {

    public interface OnItemPickedListener {
        void onItemPicked(DocumentSnapshot item);
    }

    private List<DocumentSnapshot> allItems;
    private List<DocumentSnapshot> filteredItems;
    private OnItemPickedListener listener;

    public ItemPickerAdapter(List<DocumentSnapshot> items, OnItemPickedListener listener) {
        this.allItems = new ArrayList<>(items);
        this.filteredItems = new ArrayList<>(items);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_picker_dialog_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = filteredItems.get(position);
        String name = doc.getString("name");
        String brand = doc.getString("brand");
        String category = doc.getString("category");
        String imageUrl = doc.getString("imageUrl");
        Double price = doc.getDouble("price");
        Long qty = doc.getLong("quantity");
        Long threshold = doc.getLong("lowStockThreshold");

        holder.tvPickerItemName.setText(name != null ? name : "Unknown");
        holder.tvPickerBrand.setText(brand != null ? brand : "Unknown Brand");
        holder.tvPickerCategory.setText(category != null ? category : "General");
        holder.tvPickerPrice.setText(String.format(Locale.getDefault(), "₱%,.2f", price != null ? price : 0));
        holder.tvPickerStock.setText("Qty: " + (qty != null ? qty : 0));

        // STOCK BADGE COLOR
        if (qty != null && threshold != null && qty <= threshold) {
            holder.tvPickerStock.setBackgroundResource(R.drawable.badge_red);
            holder.tvPickerStock.setTextColor(0xFFFFFFFF);
        } else {
            holder.tvPickerStock.setBackgroundResource(R.drawable.badge_green);
            holder.tvPickerStock.setTextColor(0xFF2E7D32);
        }

        // CATEGORY ICON
        if ("Smartphones & Tablets".equals(category)) {
            holder.tvItemPickerIcon.setText("📱");
            holder.tvItemPickerIcon.setBackgroundColor(0xFFE8F0FE);
        } else {
            holder.tvItemPickerIcon.setText("🎧");
            holder.tvItemPickerIcon.setBackgroundColor(0xFFFFF3E0);
        }

        // IMAGE LOADING
        Context context = holder.itemView.getContext();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            holder.imgItemPicker.setVisibility(View.VISIBLE);
            holder.tvItemPickerIcon.setVisibility(View.GONE);
            Glide.with(context)
                    .load(imageUrl)
                    .apply(RequestOptions.placeholderOf(R.drawable.ic_item_placeholder)
                            .error(R.drawable.ic_item_placeholder)
                            .transform(new RoundedCorners(20)))
                    .into(holder.imgItemPicker);
        } else {
            holder.imgItemPicker.setVisibility(View.GONE);
            holder.tvItemPickerIcon.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemPicked(doc));
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }

    public void filter(String query, String categoryFilter) {
        filteredItems.clear();
        String lowerQuery = query.toLowerCase();
        for (DocumentSnapshot doc : allItems) {
            String name = doc.getString("name");
            String brand = doc.getString("brand");
            String category = doc.getString("category");

            boolean matchesQuery = query.isEmpty() ||
                    (name != null && name.toLowerCase().contains(lowerQuery)) ||
                    (brand != null && brand.toLowerCase().contains(lowerQuery));
            boolean matchesCategory = categoryFilter.equals("All") ||
                    (category != null && category.equals(categoryFilter));

            if (matchesQuery && matchesCategory) {
                filteredItems.add(doc);
            }
        }
        notifyDataSetChanged();
    }

    public int getFilteredCount() {
        return filteredItems.size();
    }

    public void updateList(List<DocumentSnapshot> newList) {
        this.allItems = new ArrayList<>(newList);
        this.filteredItems = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgItemPicker;
        TextView tvItemPickerIcon, tvPickerItemName, tvPickerBrand,
                tvPickerCategory, tvPickerStock, tvPickerPrice;

        ViewHolder(View itemView) {
            super(itemView);
            imgItemPicker = itemView.findViewById(R.id.imgItemPicker);
            tvItemPickerIcon = itemView.findViewById(R.id.tvItemPickerIcon);
            tvPickerItemName = itemView.findViewById(R.id.tvPickerItemName);
            tvPickerBrand = itemView.findViewById(R.id.tvPickerBrand);
            tvPickerCategory = itemView.findViewById(R.id.tvPickerCategory);
            tvPickerStock = itemView.findViewById(R.id.tvPickerStock);
            tvPickerPrice = itemView.findViewById(R.id.tvPickerPrice);
        }
    }
}
