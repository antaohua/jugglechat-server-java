package com.juggle.chat.apimodels;

public enum FileType {
    DEFAULT(0),
    IMAGE(1),
    AUDIO(2),
    VIDEO(3),
    FILE(4),
    LOG(5);

    private final int code;

    FileType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static FileType fromCode(int code) {
        for (FileType value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return DEFAULT;
    }
}
