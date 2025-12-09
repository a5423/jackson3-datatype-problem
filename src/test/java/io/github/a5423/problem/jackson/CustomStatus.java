/**
 * MIT License
 * Copyright (c) 2015-2025 Willi Schönborn <w.schoenborn@gmail.com>
 */
package io.github.a5423.problem.jackson;

import org.zalando.problem.StatusType;

enum CustomStatus implements StatusType {

    @SuppressWarnings("unused")
    OK(200, "OK");

    private final int statusCode;
    private final String reasonPhrase;

    CustomStatus(final int statusCode, final String reasonPhrase) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getReasonPhrase() {
        return reasonPhrase;
    }

}
