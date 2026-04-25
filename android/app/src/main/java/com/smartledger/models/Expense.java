package com.smartledger.models;

import java.util.Date;

public class Expense {
    private String id;
    private String userId;
    private double amount;
    private String category;
    private String description;
    private Date date;
    private Date createdAt;

    public Expense() {}

    public Expense(String userId, double amount, String category, String description, Date date) {
        this.userId = userId;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.date = date;
        this.createdAt = new Date();
    }

    public Expense(String id, String userId, double amount, String category, String description, Date date, Date createdAt) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.date = date;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public Date getDate() { return date; }
    public Date getCreatedAt() { return createdAt; }
}
