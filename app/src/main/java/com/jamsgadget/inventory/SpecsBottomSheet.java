package com.jamsgadget.inventory;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SpecsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_SPECS = "specs";
    private static final String ARG_CATEGORY = "category";

    public static SpecsBottomSheet newInstance(Map<String, String> specs, String category) {
        SpecsBottomSheet fragment = new SpecsBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SPECS, (Serializable) specs);
        args.putString(ARG_CATEGORY, category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog != null) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setPeekHeight(400); 
                behavior.setDraggable(true);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_specs_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout layoutSpecsList = view.findViewById(R.id.layoutSpecsList);
        TextView tvNoSpecs = view.findViewById(R.id.tvNoSpecs);
        view.findViewById(R.id.btnCloseSpecs).setOnClickListener(v -> dismiss());

        if (getArguments() == null) return;
        Map<String, String> specs = (Map<String, String>) getArguments().getSerializable(ARG_SPECS);
        String category = getArguments().getString(ARG_CATEGORY);

        if (specs == null || specs.isEmpty()) {
            tvNoSpecs.setVisibility(View.VISIBLE);
            layoutSpecsList.setVisibility(View.GONE);
        } else {
            tvNoSpecs.setVisibility(View.GONE);
            layoutSpecsList.setVisibility(View.VISIBLE);
            populateSpecs(layoutSpecsList, specs, category);
        }
    }

    private void populateSpecs(LinearLayout container, Map<String, String> specs, String category) {
        String[] displayOrder;
        // Updated category check to be more robust
        boolean isIphoneOrTablet = category != null && (category.contains("Iphones") || category.contains("Smartphones") || category.contains("Tablets"));
        
        if (isIphoneOrTablet) {
            displayOrder = new String[]{"display", "ram", "storage", "battery", "processor", "os", "camera", "color", "condition"};
        } else {
            displayOrder = new String[]{"type", "compatible", "connectivity", "wattage", "color", "warranty", "condition"};
        }

        Map<String, String> labels = new HashMap<>();
        labels.put("display", "Display Size");
        labels.put("ram", "RAM");
        labels.put("storage", "Storage");
        labels.put("battery", "Battery");
        labels.put("processor", "Processor");
        labels.put("os", "Operating System");
        labels.put("camera", "Main Camera");
        labels.put("color", "Color / Variant");
        labels.put("condition", "Condition");
        labels.put("type", "Accessory Type");
        labels.put("compatible", "Compatible With");
        labels.put("connectivity", "Connectivity");
        labels.put("wattage", "Wattage / Power");
        labels.put("warranty", "Warranty");

        for (int i = 0; i < displayOrder.length; i++) {
            String key = displayOrder[i];
            if (specs.containsKey(key)) {
                String value = specs.get(key);
                if (value != null && !value.isEmpty()) {
                    addSpecRow(container, labels.get(key), value, key);
                    
                    // Add a divider if it's not the last visible item
                    View divider = new View(getContext());
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
                    divider.setLayoutParams(lp);
                    divider.setBackgroundColor(Color.parseColor("#F5F5F5"));
                    container.addView(divider);
                }
            }
        }
    }

    private void addSpecRow(LinearLayout container, String label, String value, String key) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 32, 0, 32);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvLabel = new TextView(getContext());
        tvLabel.setText(label);
        tvLabel.setTextColor(Color.parseColor("#888888"));
        tvLabel.setTextSize(13);
        LinearLayout.LayoutParams lpLabel = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tvLabel.setLayoutParams(lpLabel);

        View valueView;
        if (key.equals("condition") || key.equals("warranty")) {
            TextView tvBadge = new TextView(getContext());
            tvBadge.setText(value);
            tvBadge.setTextSize(11);
            tvBadge.setPadding(24, 8, 24, 8);
            tvBadge.setGravity(Gravity.CENTER);
            tvBadge.setTypeface(null, android.graphics.Typeface.BOLD);
            applyBadgePillStyle(tvBadge, value);
            valueView = tvBadge;
        } else {
            TextView tvValue = new TextView(getContext());
            tvValue.setText(value);
            tvValue.setTextColor(Color.parseColor("#212121"));
            tvValue.setTextSize(13);
            tvValue.setGravity(Gravity.END);
            tvValue.setTypeface(null, android.graphics.Typeface.BOLD);
            valueView = tvValue;
        }

        row.addView(tvLabel);
        row.addView(valueView);
        container.addView(row);
    }

    private void applyBadgePillStyle(TextView tv, String value) {
        String bgColor;
        String textColor;

        if (value == null) value = "";

        switch (value) {
            case "Brand New":
                bgColor = "#E8F5E9"; textColor = "#2E7D32"; break;
            case "Refurbished":
                bgColor = "#FFF3E0"; textColor = "#E65100"; break;
            case "Open Box":
                bgColor = "#E3F2FD"; textColor = "#1565C0"; break;
            case "No Warranty":
                bgColor = "#F5F5F5"; textColor = "#757575"; break;
            default:
                bgColor = "#F3E5F5"; textColor = "#4A148C"; break;
        }
        
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(100);
        shape.setColor(Color.parseColor(bgColor));
        tv.setBackground(shape);
        tv.setTextColor(Color.parseColor(textColor));
    }
}
