package com.juggle.chat.apimodels;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PreSignResp {
    @JsonProperty("url")
    private String url;

    @JsonProperty("obj_key")
    private String objKey;

    @JsonProperty("policy")
    private String policy;

    @JsonProperty("sign_version")
    private String signVersion;

    @JsonProperty("credential")
    private String credential;

    @JsonProperty("date")
    private String date;

    @JsonProperty("signature")
    private String signature;
}
