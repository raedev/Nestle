package com.github.raedev.nestle;

/**
 * Created by rae on 2020/9/4.
 * Copyright (c) https://github.com/raedev All rights reserved.
 */
public class NestleRuntimeException extends RuntimeException {

    private static final String EXCEPTION_PREFIX = "Nestle Exception:";

    public NestleRuntimeException() {
    }

    public NestleRuntimeException(String message) {
        super(EXCEPTION_PREFIX + message);
    }

    public NestleRuntimeException(String message, Throwable cause) {
        super(EXCEPTION_PREFIX + message, cause);
    }
}
