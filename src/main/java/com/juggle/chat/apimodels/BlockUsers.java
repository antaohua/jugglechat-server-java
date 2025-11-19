package com.juggle.chat.apimodels;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class BlockUsers {
    @JsonProperty("items")
    private List<UserInfo> items = new ArrayList<>();

    @JsonProperty("offset")
    private String offset;

    public void addUser(UserInfo user) {
        if (user == null) {
            return;
        }
        this.items.add(user);
    }
}
