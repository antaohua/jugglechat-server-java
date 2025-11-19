package com.juggle.chat.apimodels;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class AssistantPrompts {
    @JsonProperty("items")
    private List<AssistantPrompt> items;

    @JsonProperty("offset")
    private String offset;
}
