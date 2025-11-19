package com.juggle.chat.models;

import lombok.Data;

@Data
public class QrCodeRecord {
    public static final int STATUS_DEFAULT = 0;
    public static final int STATUS_OK = 1;

    private Long id;
    private String codeId;
    private Integer status;
    private Long createdTime;
    private String userId;
    private String appkey;
}
