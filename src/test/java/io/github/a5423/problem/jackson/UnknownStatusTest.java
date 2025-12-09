/**
 * MIT License
 * Copyright (c) 2015-2025 Willi Schönborn <w.schoenborn@gmail.com>
 * Copyright (c) 2025 a5423
 */
package io.github.a5423.problem.jackson;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnknownStatusTest {

    @Test
    void shouldReturnCodeAndPhrase() {
        final int code = 8080;
        final UnknownStatus status = new UnknownStatus(code);
        assertEquals(8080, status.getStatusCode());
        assertEquals("Unknown", status.getReasonPhrase());
    }

}