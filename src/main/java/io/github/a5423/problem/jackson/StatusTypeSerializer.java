/**
 * MIT License
 * Copyright (c) 2015-2025 Willi Schönborn <w.schoenborn@gmail.com>
 * Copyright (c) 2025 a5423
 */
package io.github.a5423.problem.jackson;

import org.zalando.problem.StatusType;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

final class StatusTypeSerializer extends ValueSerializer<StatusType> {

    @Override
    public void serialize(final StatusType status, final JsonGenerator json, final SerializationContext serializers) {
        json.writeNumber(status.getStatusCode());
    }

}
