package com.jamsgadget.inventory.model;

import com.google.firebase.Timestamp;
import java.util.Map;

public class Item {
    private String id;
    private String userId; // Added this
    private String name;
    private String category;
    private String brand;
    private String description;
    private String imageUrl;
    private double price;
    private int quantity;
    private int lowStockThreshold;
    private Timestamp timestamp; // Changed from dateAdded to match Firestore field
    private Map<String, String> specs;

    public Item() {
        // Required by Firestore
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    // Keep getter for backward compatibility if needed elsewhere
    public Timestamp getDateAdded() { return timestamp; }

    public Map<String, String> getSpecs() { return specs; }
    public void setSpecs(Map<String, String> specs) { this.specs = specs; }

    public String getCondition() {
        if (specs != null && specs.containsKey("condition")) {
            return specs.get("condition");
        }
        return null;
    }

    public boolean isLowStock() {
        return quantity <= lowStockThreshold;
    }
}
