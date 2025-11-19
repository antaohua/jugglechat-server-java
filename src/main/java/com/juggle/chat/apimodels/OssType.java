package com.juggle.chat.apimodels;

public enum OssType {
    DEFAULT(0),
    QINIU(1),
    S3(2),
    MINIO(3),
    OSS(4);

    private final int code;

    OssType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
