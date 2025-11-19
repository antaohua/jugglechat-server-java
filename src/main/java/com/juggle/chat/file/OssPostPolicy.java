package com.juggle.chat.file;

import lombok.Data;

@Data
public class OssPostPolicy {
    private String objKey;
    private String policy;
    private String signVersion;
    private String credential;
    private String date;
    private String signature;
}
