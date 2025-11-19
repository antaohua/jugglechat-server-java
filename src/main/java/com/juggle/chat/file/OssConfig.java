package com.juggle.chat.file;

import lombok.Data;

@Data
public class OssConfig {
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private String bucket;
    private String region;
}
