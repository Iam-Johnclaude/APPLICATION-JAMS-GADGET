package com.jamsgadget.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private TextView tvEmpty;
    private NotificationAdapter adapter;
    private List<NotificationItem> notificationList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();
        rvNotifications = findViewById(R.id.rvNotifications);
        tvEmpty = findViewById(R.id.tvEmpty);

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        loadNotifications();
    }

    private void loadNotifications() {
        notificationList.clear();
        loadDueDateNotifications();
    }

    private void loadDueDateNotifications() {
        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, 7);
        Date soon = cal.getTime();

        db.collection("installments")
                .whereEqualTo("status", "Active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Timestamp nextDueTs = doc.getTimestamp("nextDueDate");
                        if (nextDueTs != null) {
                            Date nextDueDate = nextDueTs.toDate();
                            String id = doc.getId();
                            String customer = doc.getString("customerName");
                            String item = doc.getString("itemName");
                            
                            if (nextDueDate.after(now) && nextDueDate.before(soon)) {
                                notificationList.add(new NotificationItem(
                                        id,
                                        "Upcoming Due Date",
                                        "Installment for " + item + " (" + customer + ") is due on " + sdf.format(nextDueDate),
                                        NotificationType.DUE_DATE
                                ));
                            } else if (nextDueDate.before(now)) {
                                notificationList.add(new NotificationItem(
                                        id,
                                        "Overdue Payment",
                                        "Installment for " + item + " (" + customer + ") was due on " + sdf.format(nextDueDate),
                                        NotificationType.OVERDUE
                                ));
                            }
                        }
                    }
                    loadLowStockNotifications();
                });
    }

    private void loadLowStockNotifications() {
        db.collection("items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Long qtyLong = doc.getLong("quantity");
                        Long thresholdLong = doc.getLong("lowStockThreshold");
                        String name = doc.getString("name");
                        String id = doc.getId();
                        
                        int qty = qtyLong != null ? qtyLong.intValue() : 0;
                        int threshold = thresholdLong != null ? thresholdLong.intValue() : 5;

                        if (qty <= threshold) {
                            notificationList.add(new NotificationItem(
                                    id,
                                    "Low Stock Alert",
                                    "Only " + qty + " units left of " + name + ". Restock soon!",
                                    NotificationType.LOW_STOCK
                            ));
                        }
                    }

                    if (notificationList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    enum NotificationType {
        DUE_DATE, OVERDUE, LOW_STOCK
    }

    static class NotificationItem {
        String id;
        String title;
        String message;
        NotificationType type;

        NotificationItem(String id, String title, String message, NotificationType type) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.type = type;
        }
    }

    static class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        private List<NotificationItem> items;

        NotificationAdapter(List<NotificationItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotificationItem item = items.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvMessage.setText(item.message);
            
            int color;
            int iconRes = R.drawable.ic_notification;
            
            switch (item.type) {
                case OVERDUE:
                    color = holder.itemView.getContext().getColor(R.color.brand_red);
                    iconRes = R.drawable.ic_alert;
                    break;
                case LOW_STOCK:
                    color = holder.itemView.getContext().getColor(R.color.brand_red);
                    iconRes = R.drawable.ic_notification;
                    break;
                case DUE_DATE:
                default:
                    color = holder.itemView.getContext().getColor(R.color.brand_gold);
                    iconRes = R.drawable.ic_notification;
                    break;
            }

            holder.ivIcon.setBackgroundResource(R.drawable.bg_icon_box);
            if (holder.ivIcon.getBackground() != null) {
                holder.ivIcon.getBackground().setTint(color);
            }
            holder.ivIcon.setImageResource(iconRes);
            holder.ivIcon.setColorFilter(android.graphics.Color.WHITE);

            // Handle Click
            holder.itemView.setOnClickListener(v -> {
                Intent intent;
                if (item.type == NotificationType.LOW_STOCK) {
                    intent = new Intent(v.getContext(), ItemDetailActivity.class);
                    intent.putExtra("itemId", item.id);
                } else {
                    intent = new Intent(v.getContext(), InstallmentDetailActivity.class);
                    intent.putExtra("installmentId", item.id);
                }
                v.getContext().startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvMessage;
            android.widget.ImageView ivIcon;

            ViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvNotifTitle);
                tvMessage = itemView.findViewById(R.id.tvNotifMessage);
                ivIcon = itemView.findViewById(R.id.ivNotifIcon);
            }
        }
    }
}
