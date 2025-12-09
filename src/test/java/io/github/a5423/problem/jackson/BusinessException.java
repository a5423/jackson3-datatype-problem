/**
 * MIT License
 * Copyright (c) 2015-2025 Willi Schönborn <w.schoenborn@gmail.com>
 */
package io.github.a5423.problem.jackson;

abstract class BusinessException extends Exception {

    BusinessException(final String message) {
        super(message);
    }

}
