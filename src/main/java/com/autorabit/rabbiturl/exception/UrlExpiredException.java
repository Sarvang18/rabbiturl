package com.autorabit.rabbiturl.exception;

public class UrlExpiredException extends RuntimeException {

    public UrlExpiredException(String shortCode) {
        super("URL has expired or is inactive: " + shortCode);
    }
}
