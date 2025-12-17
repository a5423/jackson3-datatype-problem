/**
 * MIT License
 * Copyright (c) 2015-2025 Willi Schönborn <w.schoenborn@gmail.com>
 */
package io.github.a5423.problem.jackson;

import org.junit.jupiter.api.Test;
import org.zalando.problem.AbstractThrowableProblem;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.zalando.problem.Status.BAD_REQUEST;

final class EnforceCoverageTest {

    @Test
    void shouldUseMixinConstructor() {
        assertThatThrownBy(() -> {
            final URI type = URI.create("https://example.org");
            new AbstractThrowableProblemMixIn(type, "Bad Request", BAD_REQUEST, null, null, null) {
                @Override
                void set(final String key, final Object value) {
                }
            };
        }).isInstanceOf(AbstractThrowableProblem.class);
    }

}
