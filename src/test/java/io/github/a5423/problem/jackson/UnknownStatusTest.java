/**
 * MIT License
 * Copyright (c) 2015-2025 Willi Schönborn <w.schoenborn@gmail.com>
 * Copyright (c) 2025 a5423
 */
package io.github.a5423.problem.jackson;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class UnknownStatusTest {

    @Test
    void shouldReturnCodeAndPhrase() {
        final int code = 8080;
        final UnknownStatus status = new UnknownStatus(code);

        Assertions.assertThat(status)
                        .satisfies(v->{
                            assertThat(v.getStatusCode()).isEqualTo(8080);
                            assertThat(v.getReasonPhrase()).isEqualTo("Unknown");
                        });
    }

}