package com.urlshortener.api.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.urlshortener.api.exception.InvalidUrlException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LongUrlValidatorTest {

    private final LongUrlValidator validator = new LongUrlValidator();

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com", "http://example.com/path?q=1"})
    void acceptsHttpAndHttps(String url) {
        assertThatCode(() -> validator.validate(url)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "javascript:alert(1)",
                "data:text/html,<script>alert(1)</script>",
                "ftp://example.com/file",
                "not a url"
            })
    void rejectsNonHttpSchemesAndMalformedInput(String url) {
        assertThatThrownBy(() -> validator.validate(url)).isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void rejectsUrlWithoutHost() {
        assertThatThrownBy(() -> validator.validate("https:///no-host")).isInstanceOf(InvalidUrlException.class);
    }
}
