/**
 * MIT License
 * Copyright (c) 2015-2025 Willi Schönborn <w.schoenborn@gmail.com>
 * Copyright (c) 2025 a5423
 */
package io.github.a5423.problem.jackson;

import tools.jackson.databind.util.StdConverter;
import org.zalando.problem.Problem;

import java.net.URI;

final class ProblemTypeConverter extends StdConverter<URI, URI> {

    @Override
    public URI convert(final URI value) {
        return Problem.DEFAULT_TYPE.equals(value) ? null : value;
    }

}
