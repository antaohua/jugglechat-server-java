package com.juggle.chat.apimodels;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class TelegramBots {
    @JsonProperty("items")
    private List<TelegramBot> items;

    @JsonProperty("offset")
    private String offset;
}
