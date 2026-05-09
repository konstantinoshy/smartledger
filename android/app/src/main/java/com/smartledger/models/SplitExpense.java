package com.smartledger.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "split_expenses")
public class SplitExpense {
    @PrimaryKey
    @NonNull
    private String id;
    private String groupId;
    private String paidBy;
    private double amount;
    private String description;
    private boolean settled;
    private Date createdAt;

    public SplitExpense() {
        this.id = java.util.UUID.randomUUID().toString();
    }

    @Ignore
    public SplitExpense(String id, String groupId, String paidBy, double amount,
                        String description, boolean settled, Date createdAt) {
        this.id = id;
        this.groupId = groupId;
        this.paidBy = paidBy;
        this.amount = amount;
        this.description = description;
        this.settled = settled;
        this.createdAt = createdAt;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getPaidBy() { return paidBy; }
    public void setPaidBy(String paidBy) { this.paidBy = paidBy; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isSettled() { return settled; }
    public void setSettled(boolean settled) { this.settled = settled; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
