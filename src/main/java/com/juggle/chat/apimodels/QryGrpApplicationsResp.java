package com.juggle.chat.apimodels;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class QryGrpApplicationsResp {
    @JsonProperty("items")
    private List<GrpApplicationItem> items;

    public void addItem(GrpApplicationItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }
}
