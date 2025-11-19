package com.juggle.chat.file;

import com.qiniu.util.Auth;

public class QiNiuStorage {
    private final String accessKey;
    private final String secretKey;
    private final String bucket;
    private final String domain;

    public QiNiuStorage(QiNiuConfig config) {
        this.accessKey = config.getAccessKey();
        this.secretKey = config.getSecretKey();
        this.bucket = config.getBucket();
        this.domain = config.getDomain();
    }

    public String createUploadToken() {
        Auth auth = Auth.create(accessKey, secretKey);
        return auth.uploadToken(bucket);
    }

    public String getDomain() {
        return domain;
    }
}
