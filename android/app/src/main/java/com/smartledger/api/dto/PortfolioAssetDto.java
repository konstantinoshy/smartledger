package com.smartledger.api.dto;

import com.google.gson.annotations.SerializedName;

public class PortfolioAssetDto {
    public String id;

    @SerializedName("user_id")
    public String userId;

    public String symbol;
    public String name;
    public double quantity;

    @SerializedName("average_price")
    public double averagePrice;

    @SerializedName("asset_type")
    public String assetType;

    @SerializedName("updated_at")
    public String updatedAt;
}
