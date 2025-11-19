package com.juggle.chat.models;

import lombok.Data;

@Data
public class PostReaction {
    private Long id;
    private String busId;
    private Integer busType;
    private String itemKey;
    private String itemValue;
    private Long createdTime;
    private String userId;
    private String appkey;
}
