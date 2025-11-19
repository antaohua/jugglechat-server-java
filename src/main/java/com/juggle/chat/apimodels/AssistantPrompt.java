package com.juggle.chat.apimodels;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class AssistantPrompt {
    @JsonProperty("prompt_id")
    private String promptId;

    @JsonProperty("prompts")
    private String prompts;

    @JsonProperty("created_time")
    private long createdTime;
}
