package com.juggle.chat.file;

import lombok.Data;

@Data
public class QiNiuConfig {
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String domain;
}
