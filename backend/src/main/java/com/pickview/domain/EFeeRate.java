package com.pickview.domain;

public enum EFeeRate {
    MOCK_CHANNEL(3),
    PLATFORM(15);

    private final int mPercent;

    EFeeRate(int percent) {
        mPercent = percent;
    }

    public int getPercent() {
        return mPercent;
    }
}
