package com.pickview.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.pickview.api.ApiFailure;
import org.springframework.http.HttpStatus;

public final class WonAmount {

    private static final int PERCENT_DENOMINATOR = 100;
    private final int mWon;

    @JsonCreator
    public WonAmount(int won) {
        if (won < 0) {
            throw new ApiFailure(HttpStatus.BAD_REQUEST, "Amount cannot be negative");
        }
        mWon = won;
    }

    @JsonValue
    public int getWon() {
        return mWon;
    }

    public WonAmount add(WonAmount other) {
        return new WonAmount(Math.addExact(mWon, other.mWon));
    }

    public WonAmount subtract(WonAmount other) {
        return new WonAmount(Math.subtractExact(mWon, other.mWon));
    }

    public WonAmount calculateFee(EFeeRate rate) {
        return new WonAmount(Math.toIntExact(((long) mWon * rate.getPercent()) / PERCENT_DENOMINATOR));
    }

    public boolean exceeds(WonAmount other) {
        return mWon > other.mWon;
    }
}
