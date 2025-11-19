package com.juggle.chat.apimodels;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Posts {
    @JsonProperty("items")
    private List<Post> items;

    @JsonProperty("is_finished")
    private boolean finished;
}
