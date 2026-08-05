package com.urlshortener.api.exception;

/** Distinguishes "this code existed but is expired/deactivated" from a plain 404. */
public class UrlGoneException extends RuntimeException {
    public UrlGoneException(String shortCode) {
        super("Link '" + shortCode + "' has expired or was deactivated");
    }
}
