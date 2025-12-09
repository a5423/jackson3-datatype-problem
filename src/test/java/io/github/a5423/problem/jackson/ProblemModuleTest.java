/**
 * MIT License
 * Copyright (c) 2015-2025 Willi Schönborn <w.schoenborn@gmail.com>
 * Copyright (c) 2025 a5423
 */
package io.github.a5423.problem.jackson;

import org.junit.jupiter.api.Test;
import org.zalando.problem.Status;

import static org.junit.jupiter.api.Assertions.assertThrows;

final class ProblemModuleTest {

    @Test
    void defaultConstructorShouldBuildIndexCorrectly() {
        new ProblemModule();
    }

    @Test
    void shouldThrowForDuplicateStatusCode() {
        assertThrows(IllegalArgumentException.class, () -> new ProblemModule(Status.class, CustomStatus.class));
    }

}
