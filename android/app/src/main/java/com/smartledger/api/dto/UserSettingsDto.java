package com.smartledger.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO για τον πίνακα user_settings στο Supabase.
 * Αποθηκεύει το μηνιαίο budget ανά χρήστη στο cloud.
 */
public class UserSettingsDto {

    @SerializedName("user_id")
    public String userId;

    @SerializedName("monthly_budget_minor")
    public int monthlyBudgetMinor;

    @SerializedName("currency")
    public String currency;

    @SerializedName("updated_at")
    public String updatedAt;
}
