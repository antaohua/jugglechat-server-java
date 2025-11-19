package com.juggle.chat.apimodels;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PostComments {
    @JsonProperty("items")
    private List<PostComment> items;

    @JsonProperty("is_finished")
    private boolean finished;
}
