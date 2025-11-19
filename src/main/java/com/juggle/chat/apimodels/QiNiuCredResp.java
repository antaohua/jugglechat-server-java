package com.juggle.chat.apimodels;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class QiNiuCredResp {
    @JsonProperty("domain")
    private String domain;

    @JsonProperty("token")
    private String token;
}
