package com.urlshortener.api.validation;

import com.urlshortener.api.config.AppProperties;
import com.urlshortener.api.exception.InvalidUrlException;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Format (charset/length) is enforced by {@code @Pattern} on {@code CreateUrlRequest}; this
 * covers the business rule that a custom alias can't shadow a real route like {@code /api} or
 * {@code /r} (see the routing decision in the HLD, {@code /r/{shortCode}} vs a bare root path).
 */
@Component
public class AliasValidator {

    private final Set<String> reserved;

    public AliasValidator(AppProperties properties) {
        this.reserved = Set.copyOf(properties.reservedAliases());
    }

    public void validate(String alias) {
        if (reserved.contains(alias.toLowerCase())) {
            throw new InvalidUrlException("'" + alias + "' is a reserved word and can't be used as an alias");
        }
    }
}
