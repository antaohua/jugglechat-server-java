package com.juggle.chat.apimodels;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class CheckGroupMembersResp {
    @JsonProperty("group_id")
    private String groupId;

    @JsonProperty("member_exist_map")
    private Map<String, Boolean> memberExistMap;

    public void putExist(String userId, boolean exist) {
        if (this.memberExistMap == null) {
            this.memberExistMap = new HashMap<>();
        }
        this.memberExistMap.put(userId, exist);
    }
}
