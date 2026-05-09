package com.smartledger.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "portfolio_assets")
public class PortfolioAsset {
    @PrimaryKey
    @NonNull
    private String id;
    private String userId;
    private String symbol;
    private String name;
    private double quantity;
    private double averagePrice;
    private String assetType;
    private Date updatedAt;

    // Transient — δεν αποθηκεύεται στη Room, υπολογίζεται real-time
    @Ignore
    private double currentPrice;
    @Ignore
    private double change24h;

    public PortfolioAsset() {
        this.id = java.util.UUID.randomUUID().toString();
    }

    @Ignore
    public PortfolioAsset(String userId, String symbol, String name, double quantity,
                          double averagePrice, String assetType) {
        this.id = java.util.UUID.randomUUID().toString();
        this.userId = userId;
        this.symbol = symbol;
        this.name = name;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.assetType = assetType;
        this.updatedAt = new Date();
    }

    @Ignore
    public PortfolioAsset(String id, String userId, String symbol, String name,
                          double quantity, double averagePrice, String assetType, Date updatedAt) {
        this.id = id;
        this.userId = userId;
        this.symbol = symbol;
        this.name = name;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.assetType = assetType;
        this.updatedAt = updatedAt;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public double getAveragePrice() { return averagePrice; }
    public void setAveragePrice(double averagePrice) { this.averagePrice = averagePrice; }
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    // Transient accessors
    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public double getChange24h() { return change24h; }
    public void setChange24h(double change24h) { this.change24h = change24h; }

    /** Τρέχουσα αξία holdings = quantity × currentPrice */
    @Ignore
    public double getCurrentValue() { return quantity * currentPrice; }

    /** P&L = (currentPrice - averagePrice) × quantity */
    @Ignore
    public double getProfitLoss() { return (currentPrice - averagePrice) * quantity; }
}
