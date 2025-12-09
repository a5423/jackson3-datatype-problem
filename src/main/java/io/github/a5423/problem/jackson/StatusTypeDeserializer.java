/**
 * MIT License
 * Copyright (c) 2015-2025 Willi Schönborn <w.schoenborn@gmail.com>
 * Copyright (c) 2025 a5423
 */
package io.github.a5423.problem.jackson;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.zalando.problem.StatusType;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.util.Map;

final class StatusTypeDeserializer extends ValueDeserializer<StatusType> {

    private final Map<Integer, StatusType> index;

    StatusTypeDeserializer(final Map<Integer, StatusType> index) {
        this.index = index;
    }

    @Override
    public StatusType deserialize(final JsonParser json, final DeserializationContext context) {
        final int statusCode = json.getIntValue();
        @Nullable final StatusType status = index.get(statusCode);
        return status == null ? new UnknownStatus(statusCode) : status;
    }

}
