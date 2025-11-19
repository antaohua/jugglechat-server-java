package com.juggle.chat.apimodels;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PostContentImage {
    @JsonProperty("url")
    private String url;

    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;

    @JsonProperty("height")
    private Integer height;

    @JsonProperty("width")
    private Integer width;
}
