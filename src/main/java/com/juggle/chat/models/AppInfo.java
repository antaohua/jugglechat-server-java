package com.juggle.chat.models;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;


@Data
@Getter
@Setter
public class AppInfo {
    private Long id;
    private String appName;
    private String appkey;
    private String appSecret;
    private String appSecureKey;
    private Integer appStatus;
    private Integer appType;
    private Date createdTime;
    private Date updatedTime;
}
