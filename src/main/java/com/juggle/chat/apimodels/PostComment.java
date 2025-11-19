package com.juggle.chat.apimodels;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PostComment {
    @JsonProperty("comment_id")
    private String commentId;

    @JsonProperty("post_id")
    private String postId;

    @JsonProperty("parent_comment_id")
    private String parentCommentId;

    @JsonProperty("text")
    private String text;

    @JsonProperty("parent_user_id")
    private String parentUserId;

    @JsonProperty("parent_user_info")
    private UserInfo parentUserInfo;

    @JsonProperty("user_info")
    private UserInfo userInfo;

    @JsonProperty("created_time")
    private long createdTime;

    @JsonProperty("updated_time")
    private long updatedTime;
}
