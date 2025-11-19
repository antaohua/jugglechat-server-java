package com.juggle.chat.apimodels;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PostCommentIds {
    @JsonProperty("comment_ids")
    private List<String> commentIds;
}
