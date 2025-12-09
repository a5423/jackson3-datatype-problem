/**
 * MIT License
 * Copyright (c) 2015-2025 Willi Schönborn <w.schoenborn@gmail.com>
 * Copyright (c) 2025 a5423
 */
package io.github.a5423.problem.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.zalando.problem.ThrowableProblem;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonIgnoreProperties(ignoreUnknown = true)
interface ExceptionalMixin {

    @JsonIgnore
    String getMessage();

    @JsonIgnore
    String getLocalizedMessage();

    @JsonInclude(NON_NULL)
    ThrowableProblem getCause();

    // decision about inclusion is up to derived mixins
    @JsonProperty("stacktrace")
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    StackTraceElement[] getStackTrace();

    @JsonIgnore
    Throwable[] getSuppressed();

}
