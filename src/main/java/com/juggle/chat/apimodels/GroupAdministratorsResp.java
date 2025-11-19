package com.juggle.chat.apimodels;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class GroupAdministratorsResp {
    @JsonProperty("group_id")
    private String groupId;

    @JsonProperty("items")
    private List<GroupMemberInfo> items;

    public void addAdministrator(GroupMemberInfo info) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(info);
    }
}
