package com.juggle.chat.file;

import lombok.Data;

@Data
public class S3Config {
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private String region;
    private String bucket;
}
