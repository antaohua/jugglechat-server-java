package com.juggle.chat.apimodels;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class QryFileCredResp {
    @JsonProperty("oss_type")
    private int ossType;

    @JsonProperty("qiniu_resp")
    private QiNiuCredResp qiNiuCredResp;

    @JsonProperty("pre_sign_resp")
    private PreSignResp preSignResp;
}
