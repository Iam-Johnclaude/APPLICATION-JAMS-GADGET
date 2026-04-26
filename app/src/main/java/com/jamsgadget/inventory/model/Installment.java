package com.jamsgadget.inventory.model;

public class Installment {
    private int id;
    private String customerName;
    private String productName;
    private String financingSource;
    private double monthlyAmount;
    private double totalAmount;
    private double paidAmount;
    private String status; // "Active", "Overdue", "Paid"

    public Installment(int id, String customerName, String productName, String financingSource, double monthlyAmount, double totalAmount, double paidAmount, String status) {
        this.id = id;
        this.customerName = customerName;
        this.productName = productName;
        this.financingSource = financingSource;
        this.monthlyAmount = monthlyAmount;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getFinancingSource() { return financingSource; }
    public void setFinancingSource(String financingSource) { this.financingSource = financingSource; }

    public double getMonthlyAmount() { return monthlyAmount; }
    public void setMonthlyAmount(double monthlyAmount) { this.monthlyAmount = monthlyAmount; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}