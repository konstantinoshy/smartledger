package com.smartledger.api.dto;

import com.google.gson.annotations.SerializedName;

public class SplitGroupDto {
    public String id;

    @SerializedName("owner_id")
    public String ownerId;

    public String name;

    @SerializedName("created_at")
    public String createdAt;
}
