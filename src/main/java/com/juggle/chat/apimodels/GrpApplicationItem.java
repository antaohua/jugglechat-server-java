package com.juggle.chat.apimodels;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class GrpApplicationItem {
    @JsonProperty("application_id")
    private String applicationId;

    @JsonProperty("grp_info")
    private GroupInfo grpInfo;

    @JsonProperty("apply_type")
    private int applyType;

    @JsonProperty("sponsor")
    private UserInfo sponsor;

    @JsonProperty("recipient")
    private UserInfo recipient;

    @JsonProperty("inviter")
    private UserInfo inviter;

    @JsonProperty("operator")
    private UserInfo operator;

    @JsonProperty("apply_time")
    private long applyTime;

    @JsonProperty("status")
    private int status;
}
