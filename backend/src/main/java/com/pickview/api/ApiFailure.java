package com.pickview.api;

import org.springframework.http.HttpStatus;

public class ApiFailure extends RuntimeException {

    private final HttpStatus mStatus;

    public ApiFailure(HttpStatus status, String message) {
        super(message);
        mStatus = status;
    }

    public HttpStatus getStatus() {
        return mStatus;
    }
}
