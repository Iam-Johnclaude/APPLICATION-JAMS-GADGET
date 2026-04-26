package com.jamsgadget.inventory;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.jamsgadget.inventory.model.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportsAnalyticsActivity extends AppCompatActivity {

    private PieChart pieChart;
    private BarChart barChart;
    private TextView tvInventoryValue, tvWellStocked, tvLowStock;
    private TextView tvTotalOutstanding, tvTotalItemsLabel, tvActiveLoansLabel;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration itemsListener;
    private ListenerRegistration installmentsListener;
    
    private final int TEXT_COLOR_DARK = Color.parseColor("#1E293B"); 
    private final int TEXT_COLOR_MUTED = Color.parseColor("#64748B"); 
    private final int COLOR_PRIMARY = Color.parseColor("#1A6FC4");
    private final int COLOR_ORANGE = Color.parseColor("#E07B2A");
    private final int COLOR_GREEN = Color.parseColor("#16A34A"); 
    private final int COLOR_RED = Color.parseColor("#DC2626"); 
    private final int[] CHART_COLORS = {COLOR_PRIMARY, COLOR_ORANGE, COLOR_GREEN, COLOR_RED, Color.parseColor("#8E44AD"), Color.parseColor("#2C3E50")};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports_analytics);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        initViews();
        startListeners();
    }

    private void initViews() {
        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);
        
        tvInventoryValue = findViewById(R.id.tvInventoryValue);
        tvWellStocked = findViewById(R.id.tvWellStocked);
        tvLowStock = findViewById(R.id.tvLowStock);
        tvTotalOutstanding = findViewById(R.id.tvTotalOutstanding);
        tvTotalItemsLabel = findViewById(R.id.tvTotalItemsLabel);
        tvActiveLoansLabel = findViewById(R.id.tvActiveLoansLabel);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void startListeners() {
        startItemsListener();
        startInstallmentsListener();
    }

    private void startItemsListener() {
        itemsListener = db.collection("items").addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value != null) {
                List<Item> items = new ArrayList<>();
                for (QueryDocumentSnapshot doc : value) {
                    Item item = doc.toObject(Item.class);
                    if (item != null) {
                        item.setId(doc.getId());
                        items.add(item);
                    }
                }
                updateInventoryData(items);
            }
        });
    }

    private void startInstallmentsListener() {
        installmentsListener = db.collection("installments").addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value != null) {
                double totalOutstanding = 0;
                int activeLoans = 0;
                for (DocumentSnapshot doc : value.getDocuments()) {
                    String status = doc.getString("status");
                    if (status != null && (status.equalsIgnoreCase("Active") || status.equalsIgnoreCase("Overdue"))) {
                        activeLoans++;
                        double price = getDouble(doc, "itemPrice");
                        double paid = getDouble(doc, "totalPaid");
                        totalOutstanding += Math.max(0, price - paid);
                    }
                }
                if (tvTotalOutstanding != null) tvTotalOutstanding.setText(formatCurrency(totalOutstanding));
                if (tvActiveLoansLabel != null) tvActiveLoansLabel.setText(activeLoans + " Active Loans");
            }
        });
    }

    private double getDouble(DocumentSnapshot doc, String field) {
        Double val = doc.getDouble(field);
        return val != null ? val : 0;
    }

    private void updateInventoryData(List<Item> items) {
        double totalValue = 0;
        int lowStockCount = 0;
        int wellStockedCount = 0;
        Map<String, Integer> categoryMap = new HashMap<>();

        for (Item item : items) {
            totalValue += (item.getPrice() * item.getQuantity());
            if (item.isLowStock()) lowStockCount++;
            else wellStockedCount++;

            String category = item.getCategory();
            if (category == null || category.isEmpty()) category = "Other";
            
            if (category.toLowerCase().contains("phone") || category.toLowerCase().contains("tablet")) category = "Smartphones";
            else if (category.toLowerCase().contains("acc")) category = "Accessories";
            
            categoryMap.put(category, categoryMap.getOrDefault(category, 0) + 1);
        }

        if (tvInventoryValue != null) tvInventoryValue.setText(formatCurrency(totalValue));
        if (tvTotalItemsLabel != null) tvTotalItemsLabel.setText(items.size() + " Total Items");
        if (tvWellStocked != null) tvWellStocked.setText(wellStockedCount + " Well Stocked");
        if (tvLowStock != null) tvLowStock.setText(lowStockCount + " Low Stock");

        setupPieChart(categoryMap);
        setupBarChart(items);
    }

    private void setupPieChart(Map<String, Integer> categoryMap) {
        if (pieChart == null) return;
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : categoryMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(CHART_COLORS);
        dataSet.setValueTextSize(13f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new PercentFormatter(pieChart));

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawEntryLabels(false);
        pieChart.invalidate();
    }

    private void setupBarChart(List<Item> items) {
        if (barChart == null) return;
        
        List<Item> sortedItems = new ArrayList<>(items);
        Collections.sort(sortedItems, (o1, o2) -> Double.compare(o2.getPrice() * o2.getQuantity(), o1.getPrice() * o1.getQuantity()));
        
        int count = Math.min(sortedItems.size(), 5);
        List<BarEntry> entries = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Item item = sortedItems.get(i);
            entries.add(new BarEntry(i, (float) (item.getPrice() * item.getQuantity())));
            String shortName = item.getName();
            if (shortName.length() > 10) shortName = shortName.substring(0, 8) + "...";
            labels.add(shortName);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Total Value (Price x Stock)");
        dataSet.setColors(CHART_COLORS);
        dataSet.setValueTextColor(TEXT_COLOR_DARK);
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1_000_000) return String.format(Locale.getDefault(), "₱%.1fM", value / 1_000_000f);
                if (value >= 1_000) return String.format(Locale.getDefault(), "₱%.0fk", value / 1_000f);
                return String.format(Locale.getDefault(), "₱%.0f", value);
            }
        });

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f); 
        
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.getLegend().setEnabled(false);
        
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(count);
        xAxis.setTextColor(TEXT_COLOR_MUTED);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelRotationAngle(-30);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E2E8F0"));
        leftAxis.setTextColor(TEXT_COLOR_MUTED);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1_000_000) return (int)(value/1_000_000) + "M";
                if (value >= 1_000) return (int)(value/1_000) + "k";
                return String.valueOf((int)value);
            }
        });

        barChart.getAxisRight().setEnabled(false);
        barChart.setExtraBottomOffset(20f); 
        barChart.animateY(1000, Easing.EaseInOutQuad);
        barChart.invalidate();
    }

    private String formatCurrency(double value) {
        return String.format(Locale.getDefault(), "₱%,.0f", value);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (itemsListener != null) { itemsListener.remove(); itemsListener = null; }
        if (installmentsListener != null) { installmentsListener.remove(); installmentsListener = null; }
    }

    @Override
    protected void onDestroy() {
        if (itemsListener != null) itemsListener.remove();
        if (installmentsListener != null) installmentsListener.remove();
        super.onDestroy();
    }
}
