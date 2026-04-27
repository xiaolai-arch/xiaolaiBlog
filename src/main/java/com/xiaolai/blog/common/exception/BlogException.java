package com.xiaolai.blog.common.exception;

public class BlogException extends RuntimeException {
    private final int code;

    public BlogException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BlogException(String message) {
        super(message);
        this.code = 500;
    }

    public int getCode() {
        return code;
    }
}