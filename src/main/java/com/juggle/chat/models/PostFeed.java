package com.juggle.chat.models;

import lombok.Data;

@Data
public class PostFeed {
    private Long id;
    private String userId;
    private String postId;
    private Long feedTime;
    private String appkey;
}
