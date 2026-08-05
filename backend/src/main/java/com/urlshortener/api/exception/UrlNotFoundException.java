package com.urlshortener.api.exception;

public class UrlNotFoundException extends RuntimeException {
    public UrlNotFoundException(String shortCode) {
        super("No link found for code '" + shortCode + "'");
    }
}
