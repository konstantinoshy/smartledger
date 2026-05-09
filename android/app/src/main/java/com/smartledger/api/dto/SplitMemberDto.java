package com.smartledger.api.dto;

import com.google.gson.annotations.SerializedName;

public class SplitMemberDto {
    public String id;

    @SerializedName("group_id")
    public String groupId;

    @SerializedName("display_name")
    public String displayName;

    @SerializedName("created_at")
    public String createdAt;
}
