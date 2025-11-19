package com.juggle.chat.models;

public enum PostBusType {
    POST(0),
    COMMENT(1);

    private final int code;

    PostBusType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
