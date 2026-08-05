package com.urlshortener.api.validation;

import com.urlshortener.api.exception.InvalidUrlException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * A URL shortener is an open redirector by definition: whatever scheme it accepts, it will
 * later hand straight back in a 302. Restricting to http/https keeps it from being used to
 * launder a {@code javascript:} or {@code data:} payload through a trusted-looking short link.
 */
@Component
public class LongUrlValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    public void validate(String longUrl) {
        URI uri;
        try {
            uri = new URI(longUrl);
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("longUrl is not a valid URI");
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new InvalidUrlException("longUrl must use http or https");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new InvalidUrlException("longUrl must include a host");
        }
    }
}
