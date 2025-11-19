package com.juggle.chat.file;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class OssPostPolicy {
    private String objKey;
    private String policy;
    private String signVersion;
    private String credential;
    private String date;
    private String signature;
}
