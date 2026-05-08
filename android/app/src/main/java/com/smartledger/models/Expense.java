package com.smartledger.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "expenses")
public class Expense {
    @PrimaryKey
    @NonNull
    private String id;
    private String userId;
    private double amount;
    private String category;
    private String description;
    private Date date;
    private Date createdAt;

    public Expense() {
        this.id = java.util.UUID.randomUUID().toString();
    }

    public Expense(String userId, double amount, String category, String description, Date date) {
        this.id = java.util.UUID.randomUUID().toString();
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

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
