package com.juggle.chat.apimodels;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Post {
    @JsonProperty("post_id")
    private String postId;

    @JsonProperty("content")
    private PostContent content;

    @JsonProperty("user_info")
    private UserInfo userInfo;

    @JsonProperty("created_time")
    private long createdTime;

    @JsonProperty("updated_time")
    private long updatedTime;

    @JsonProperty("reactions")
    private Map<String, List<PostReaction>> reactions;

    @JsonProperty("top_comments")
    private List<PostComment> topComments;
}
