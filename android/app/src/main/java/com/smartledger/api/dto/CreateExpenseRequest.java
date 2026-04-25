package com.smartledger.api.dto;

import com.google.gson.annotations.SerializedName;

public class CreateExpenseRequest {
    @SerializedName("user_id")
    private final String userId;

    @SerializedName("amount_minor")
    private final int amountMinor;

    private final String currency;
    private final String category;
    private final String description;

    @SerializedName("spent_at")
    private final String spentAt;

    public CreateExpenseRequest(
            String userId,
            int amountMinor,
            String currency,
            String category,
            String description,
            String spentAt
    ) {
        this.userId = userId;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.category = category;
        this.description = description;
        this.spentAt = spentAt;
    }
}
