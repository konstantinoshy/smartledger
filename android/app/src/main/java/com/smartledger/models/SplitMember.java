package com.smartledger.models;

public class SplitMember {
    private String id;
    private String groupId;
    private String displayName;

    public SplitMember() {}

    public SplitMember(String id, String groupId, String displayName) {
        this.id = id;
        this.groupId = groupId;
        this.displayName = displayName;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
