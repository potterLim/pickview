package com.pickview.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.pickview.api.ApiFailure;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;

public enum EAccessTerm {
    PERPETUAL(0),
    ONE_WEEK(7),
    ONE_MONTH(30),
    THREE_MONTHS(90);

    private final int mDays;

    EAccessTerm(int days) {
        mDays = days;
    }

    @JsonCreator
    public static EAccessTerm parseDays(int days) {
        for (EAccessTerm term : values()) {
            if (term.mDays == days) {
                return term;
            }
        }
        throw new ApiFailure(HttpStatus.BAD_REQUEST, "Invalid access term");
    }

    @JsonValue
    public int getDays() {
        return mDays;
    }

    public long calculateExpiry(Instant purchasedAt) {
        return this == PERPETUAL ? 0 : purchasedAt.plus(mDays, ChronoUnit.DAYS).toEpochMilli();
    }
}
