package com.pickview.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.pickview.api.ApiFailure;

public final class ProductPrice {

    private static final int MIN_PAID_PRICE_WON = 1_000;
    private static final int MAX_PRICE_WON = 1_000_000;
    private static final int PRICE_STEP_WON = 100;
    private final WonAmount mAmount;

    @JsonCreator
    public ProductPrice(int won) {
        if (won < 0 || won > MAX_PRICE_WON
            || (won != 0 && (won < MIN_PAID_PRICE_WON || won % PRICE_STEP_WON != 0))) {
            throw new ApiFailure(400, "Invalid product price");
        }
        mAmount = new WonAmount(won);
    }

    public WonAmount getAmount() {
        return mAmount;
    }

    @JsonValue
    public int getWon() {
        return mAmount.getWon();
    }
}
