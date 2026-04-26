package com.jamsgadget.inventory.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.jamsgadget.inventory.R;
import com.jamsgadget.inventory.model.Item;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    public enum SortOption {
        NAME_AZ, NAME_ZA, PRICE_LOW, PRICE_HIGH, QTY_LOW, QTY_HIGH, DATE_NEWEST
    }

    public interface OnItemClickListener {
        void onItemClick(Item item);
        default void onItemDelete(Item item) {}
    }

    private List<Item> itemList = new ArrayList<>();
    private List<Item> filteredList = new ArrayList<>();
    private OnItemClickListener listener;
    private SortOption currentSort = SortOption.DATE_NEWEST;
    private String currentSearchQuery = "";
    private String currentCategoryFilter = "All";
    private boolean isDashboardMode = false;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public InventoryAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public InventoryAdapter(List<Item> filteredList, OnItemClickListener listener) {
        this.filteredList = filteredList;
        this.listener = listener;
    }

    public InventoryAdapter(OnItemClickListener listener, boolean isDashboardMode) {
        this.listener = listener;
        this.isDashboardMode = isDashboardMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isDashboardMode ? R.layout.item_recent_dashboard : R.layout.item_inventory;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = filteredList.get(position);
        holder.tvName.setText(item.getName() != null ? item.getName() : "Unnamed Item");
        
        String brandText = item.getBrand() != null ? item.getBrand() : "No Brand";
        holder.tvBrand.setText(brandText + (isDashboardMode ? "" : " • " + (item.getCondition() != null ? item.getCondition() : "Gadget")));
        
        if (holder.tvCategory != null) holder.tvCategory.setText(item.getCategory() != null ? item.getCategory() : "Uncategorized");
        
        holder.tvPrice.setText(String.format(Locale.getDefault(), "₱%,.0f", item.getPrice()));
        
        if (holder.tvQty != null) {
            holder.tvQty.setText(String.valueOf(item.getQuantity()));
            if (!isDashboardMode) {
                holder.tvQty.setOnClickListener(v -> showSetQuantityDialog(item, holder.itemView.getContext()));
            }
        }

        int qty = item.getQuantity();
        int threshold = item.getLowStockThreshold() > 0 ? item.getLowStockThreshold() : 5;
        
        if (isDashboardMode) {
            holder.tvStatus.setText(qty + " in stock");
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), 
                    qty <= threshold ? R.color.brand_red : R.color.brand_green));
        } else {
            if (qty <= 0) {
                holder.tvStatus.setText("No Stock");
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.brand_red));
            } else if (qty <= threshold) {
                holder.tvStatus.setText("Low Stock");
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.brand_red));
            } else {
                holder.tvStatus.setText("In Stock");
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.brand_green));
            }
            
            if (holder.progressBar != null) {
                int progress = Math.min(100, (qty * 100) / (Math.max(1, threshold * 4)));
                holder.progressBar.setProgress(progress);
                holder.progressBar.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), 
                        qty <= threshold ? R.color.brand_red : R.color.brand_green)));
            }
        }

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            if (holder.ivItem != null) {
                holder.tvEmoji.setVisibility(View.GONE);
                holder.ivItem.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext())
                        .load(item.getImageUrl())
                        .transform(new RoundedCorners(isDashboardMode ? 16 : 24))
                        .into(holder.ivItem);
            }
        } else {
            if (holder.ivItem != null) holder.ivItem.setVisibility(View.GONE);
            holder.tvEmoji.setVisibility(View.VISIBLE);
            String emoji = "📱";
            if (item.getCategory() != null) {
                String cat = item.getCategory().toLowerCase();
                if (cat.contains("phone")) emoji = "📱";
                else if (cat.contains("laptop")) emoji = "💻";
                else if (cat.contains("tablet")) emoji = "📟";
                else if (cat.contains("headphone")) emoji = "🎧";
                else if (cat.contains("watch")) emoji = "⌚";
                else emoji = "📦";
            }
            holder.tvEmoji.setText(emoji);
        }

        // Updated Stepper Logic: Direct +/- 1
        if (!isDashboardMode && holder.btnIncrement != null) {
            holder.btnIncrement.setOnClickListener(v -> updateFirestoreQty(item, 1));
            holder.btnDecrement.setOnClickListener(v -> updateFirestoreQty(item, -1));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    private void showSetQuantityDialog(Item item, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_enter_quantity, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etQty = dialogView.findViewById(R.id.etDialogQuantity);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        etQty.setText(String.valueOf(item.getQuantity()));
        etQty.setSelection(etQty.getText().length());

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String val = etQty.getText().toString();
            if (!val.isEmpty()) {
                try {
                    int newQty = Integer.parseInt(val);
                    db.collection("items").document(item.getId()).update("quantity", newQty);
                } catch (NumberFormatException ignored) {}
            }
            dialog.dismiss();
        });

        dialog.show();
        etQty.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    private void updateFirestoreQty(Item item, int delta) {
        int newQty = Math.max(0, item.getQuantity() + delta);
        db.collection("items").document(item.getId()).update("quantity", newQty);
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void updateList(List<Item> newList) {
        this.itemList = new ArrayList<>(newList);
        applyFilters();
    }

    public void filter(String query, String category) {
        this.currentSearchQuery = query != null ? query.toLowerCase().trim() : "";
        this.currentCategoryFilter = category != null ? category : "All";
        applyFilters();
    }

    private void applyFilters() {
        filteredList.clear();
        for (Item item : itemList) {
            String name = item.getName() != null ? item.getName().toLowerCase() : "";
            String brand = item.getBrand() != null ? item.getBrand().toLowerCase() : "";
            boolean matchesSearch = name.contains(currentSearchQuery) || brand.contains(currentSearchQuery);
            boolean matchesCategory = currentCategoryFilter.equals("All") || 
                                     (item.getCategory() != null && item.getCategory().equals(currentCategoryFilter)) ||
                                     (currentCategoryFilter.equals("Low Stock") && item.isLowStock());
            if (matchesSearch && matchesCategory) filteredList.add(item);
        }
        applySort();
        notifyDataSetChanged();
    }

    private void applySort() {
        Collections.sort(filteredList, (o1, o2) -> {
            switch (currentSort) {
                case NAME_AZ: return o1.getName().compareToIgnoreCase(o2.getName());
                case NAME_ZA: return o2.getName().compareToIgnoreCase(o1.getName());
                case PRICE_LOW: return Double.compare(o1.getPrice(), o2.getPrice());
                case PRICE_HIGH: return Double.compare(o2.getPrice(), o1.getPrice());
                case QTY_LOW: return Integer.compare(o1.getQuantity(), o2.getQuantity());
                case QTY_HIGH: return Integer.compare(o2.getQuantity(), o1.getQuantity());
                case DATE_NEWEST:
                default:
                    if (o1.getDateAdded() == null || o2.getDateAdded() == null) return 0;
                    return o2.getDateAdded().compareTo(o1.getDateAdded());
            }
        });
    }

    public void setSort(SortOption sortOption) {
        this.currentSort = sortOption;
        applySort();
        notifyDataSetChanged();
    }

    public Item getItemAt(int position) {
        if (position >= 0 && position < filteredList.size()) {
            return filteredList.get(position);
        }
        return null;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvBrand, tvCategory, tvPrice, tvQty, tvStatus, tvEmoji;
        ImageView ivItem;
        ProgressBar progressBar;
        ImageButton btnIncrement, btnDecrement;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvBrand = itemView.findViewById(R.id.tvItemBrand);
            tvCategory = itemView.findViewById(R.id.tvItemCategory);
            tvPrice = itemView.findViewById(R.id.tvItemPrice);
            tvQty = itemView.findViewById(R.id.tvItemQty);
            tvStatus = itemView.findViewById(R.id.tvStockStatus);
            tvEmoji = itemView.findViewById(R.id.tvEmojiFallback);
            ivItem = itemView.findViewById(R.id.ivItemImage);
            progressBar = itemView.findViewById(R.id.stockProgressBar);
            btnIncrement = itemView.findViewById(R.id.btnIncrementQty);
            btnDecrement = itemView.findViewById(R.id.btnDecrementQty);
        }
    }
}
