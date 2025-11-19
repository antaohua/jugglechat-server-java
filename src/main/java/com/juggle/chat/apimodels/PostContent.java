package com.juggle.chat.apimodels;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PostContent {
    @JsonProperty("text")
    private String text;

    @JsonProperty("images")
    private List<PostContentImage> images;

    @JsonProperty("video")
    private PostContentVideo video;
}
