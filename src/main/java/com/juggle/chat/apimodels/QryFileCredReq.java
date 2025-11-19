package com.juggle.chat.apimodels;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class QryFileCredReq {
    @JsonProperty("file_type")
    private int fileType;

    @JsonProperty("ext")
    private String ext;
}
