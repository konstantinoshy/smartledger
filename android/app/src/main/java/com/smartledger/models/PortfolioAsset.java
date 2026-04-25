package com.smartledger.models;

import java.util.Date;

public class PortfolioAsset {
    private String id;
    private String userId;
    private String symbol;
    private String name;
    private double quantity;
    private double averagePrice;
    private String assetType;
    private Date updatedAt;

    public PortfolioAsset() {}

    public PortfolioAsset(String userId, String symbol, String name, double quantity, double averagePrice, String assetType) {
        this.userId = userId;
        this.symbol = symbol;
        this.name = name;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.assetType = assetType;
        this.updatedAt = new Date();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public double getQuantity() { return quantity; }
    public double getAveragePrice() { return averagePrice; }
    public String getAssetType() { return assetType; }
    public Date getUpdatedAt() { return updatedAt; }
}
