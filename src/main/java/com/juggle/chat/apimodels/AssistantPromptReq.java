package com.juggle.chat.apimodels;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class AssistantPromptReq {
    @JsonProperty("prompt_id")
    private String promptId;

    @JsonProperty("prompts")
    private String prompts;
}
