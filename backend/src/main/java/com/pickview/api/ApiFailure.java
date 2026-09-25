package com.pickview.api;

public class ApiFailure extends RuntimeException {
    private final int mStatus;

    public ApiFailure(int status, String message) {
        super(message);
        mStatus = status;
    }

    public int getStatus() { return mStatus; }
}
