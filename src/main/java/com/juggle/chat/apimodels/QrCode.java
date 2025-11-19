package com.juggle.chat.apimodels;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class QrCode {
    @JsonProperty("id")
    private String id;

    @JsonProperty("qr_code")
    private String qrCode;
}
