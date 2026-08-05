package com.urlshortener.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShortCodeEncoderTest {

    private final ShortCodeEncoder encoder = new ShortCodeEncoder();

    @Test
    void encodesZeroAsFirstAlphabetCharacter() {
        assertThat(encoder.encode(0)).isEqualTo("0");
    }

    @Test
    void encodesKnownValues() {
        assertThat(encoder.encode(61)).isEqualTo("z");
        assertThat(encoder.encode(62)).isEqualTo("10");
    }

    @Test
    void rejectsNegativeIds() {
        assertThatThrownBy(() -> encoder.encode(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isDeterministic() {
        assertThat(encoder.encode(123_456_789L)).isEqualTo(encoder.encode(123_456_789L));
    }

    @Test
    void neverProducesTwoCodesForDifferentIds() {
        Set<String> seen = new HashSet<>();
        for (long id = 0; id < 100_000; id++) {
            assertThat(seen.add(encoder.encode(id))).as("collision at id=%d", id).isTrue();
        }
    }
}
