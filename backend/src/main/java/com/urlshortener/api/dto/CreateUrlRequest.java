package com.urlshortener.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * {@code alias} and {@code expiresAt} are intentionally optional: built-in constraints
 * ({@code @Pattern}, {@code @Size}) treat {@code null} as valid, so validation only kicks in
 * when the caller actually supplies a value. Reserved-word / uniqueness checks for the alias
 * are business rules, not shape rules, so they live in UrlService rather than here.
 */
public record CreateUrlRequest(
        @NotBlank(message = "longUrl is required") @Size(max = 2048, message = "longUrl must be 2048 characters or fewer") String longUrl,
        @Pattern(regexp = "^[A-Za-z0-9_-]{3,16}$", message = "alias must be 3-16 characters from [A-Za-z0-9_-]") String alias,
        Instant expiresAt) {}
