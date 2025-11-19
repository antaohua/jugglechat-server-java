package com.juggle.chat.apimodels;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PostReaction {
    @JsonProperty("post_id")
    private String postId;

    @JsonProperty("key")
    private String key;

    @JsonProperty("value")
    private String value;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("user_info")
    private UserInfo userInfo;
}
