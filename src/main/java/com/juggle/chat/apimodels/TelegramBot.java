package com.juggle.chat.apimodels;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class TelegramBot {
    @JsonProperty("bot_id")
    private String botId;

    @JsonProperty("bot_name")
    private String botName;

    @JsonProperty("bot_token")
    private String botToken;

    @JsonProperty("created_time")
    private long createdTime;
}
