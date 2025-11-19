package com.juggle.chat.apimodels;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class AssistantAnswerReq {
    @JsonProperty("question")
    private String question;

    @JsonProperty("contexts")
    private List<String> contexts;
}
