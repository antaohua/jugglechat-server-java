package com.juggle.chat.apimodels;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PostContentVideo {
    @JsonProperty("url")
    private String url;

    @JsonProperty("snapshot_url")
    private String snapshotUrl;

    @JsonProperty("duration")
    private Integer duration;

    @JsonProperty("height")
    private Integer height;

    @JsonProperty("width")
    private Integer width;
}
