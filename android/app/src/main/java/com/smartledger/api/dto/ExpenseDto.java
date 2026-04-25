package com.smartledger.api.dto;

import com.google.gson.annotations.SerializedName;

public class ExpenseDto {
    public String id;

    @SerializedName("user_id")
    public String userId;

    @SerializedName("amount_minor")
    public int amountMinor;

    public String currency;
    public String category;
    public String description;

    @SerializedName("spent_at")
    public String spentAt;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("updated_at")
    public String updatedAt;
}
