package com.smartledger.models;

import java.util.Date;

public class PortfolioTransaction {
    private String id;
    private String userId;
    private String symbol;
    private String action; // "BUY" or "SELL"
    private double quantity;
    private double price;
    private Date createdAt;

    public PortfolioTransaction() {}

    public PortfolioTransaction(String id, String userId, String symbol, String action,
                                double quantity, double price, Date createdAt) {
        this.id = id;
        this.userId = userId;
        this.symbol = symbol;
        this.action = action;
        this.quantity = quantity;
        this.price = price;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public String getSymbol() { return symbol; }
    public String getAction() { return action; }
    public double getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public Date getCreatedAt() { return createdAt; }

    /** Συνολική αξία transaction = quantity × price */
    public double getTotalValue() { return quantity * price; }
}
