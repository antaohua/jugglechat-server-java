package com.juggle.chat.apimodels;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class GroupMemberInfos {
    @JsonProperty("items")
    private List<GroupMemberInfo> items;

    @JsonProperty("offset")
    private String offset;

    public void addMember(GroupMemberInfo info) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(info);
    }
}
