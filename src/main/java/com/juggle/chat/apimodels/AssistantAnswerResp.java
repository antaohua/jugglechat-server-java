package com.juggle.chat.apimodels;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class AssistantAnswerResp {
    @JsonProperty("answer")
    private String answer;
}
