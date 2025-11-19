package com.juggle.chat.apimodels;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Groups {
    @JsonProperty("items")
    private List<GroupInfo> items;

    @JsonProperty("offset")
    private String offset;

    public void addGroup(GroupInfo info) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(info);
    }
}
