package com.smartledger.api.dto;

import com.google.gson.annotations.SerializedName;

public class SplitExpenseDto {
    public String id;

    @SerializedName("group_id")
    public String groupId;

    @SerializedName("paid_by")
    public String paidBy;

    @SerializedName("amount_minor")
    public int amountMinor;

    public String description;

    @SerializedName("is_settled")
    public boolean isSettled;

    @SerializedName("created_at")
    public String createdAt;
}
