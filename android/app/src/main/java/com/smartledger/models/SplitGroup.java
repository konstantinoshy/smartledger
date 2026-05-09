package com.smartledger.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "split_groups")
public class SplitGroup {
    @PrimaryKey
    @NonNull
    private String id;
    private String ownerId;
    private String name;
    private int memberCount;
    private int pendingCount;
    private double balanceAmount;
    private Date createdAt;

    public SplitGroup() {
        this.id = java.util.UUID.randomUUID().toString();
    }

    @Ignore
    public SplitGroup(String id, String ownerId, String name, Date createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.createdAt = createdAt;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public int getPendingCount() { return pendingCount; }
    public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }

    public double getBalanceAmount() { return balanceAmount; }
    public void setBalanceAmount(double balanceAmount) { this.balanceAmount = balanceAmount; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
