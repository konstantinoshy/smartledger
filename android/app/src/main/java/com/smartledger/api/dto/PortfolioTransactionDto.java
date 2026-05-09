package com.smartledger.api.dto;

import com.google.gson.annotations.SerializedName;

public class PortfolioTransactionDto {
    public String id;

    @SerializedName("user_id")
    public String userId;

    public String symbol;
    public String action;
    public double quantity;
    public double price;

    @SerializedName("created_at")
    public String createdAt;
}
