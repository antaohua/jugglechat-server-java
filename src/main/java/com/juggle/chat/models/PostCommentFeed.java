package com.juggle.chat.models;

import lombok.Data;

@Data
public class PostCommentFeed {
    private Long id;
    private String userId;
    private String commentId;
    private String postId;
    private Long feedTime;
    private String appkey;
}
