package com.juggle.chat.file;

import lombok.Data;

@Data
public class MinioConfig {
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private boolean useSsl;
    private String bucket;
}
