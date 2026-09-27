package com.pickview.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.pickview.api.ApiFailure;

public final class AccountId {

    private final String mValue;

    @JsonCreator
    public AccountId(String value) {
        if (value == null || value.isBlank() || value.length() > 64) {
            throw new ApiFailure(400, "Invalid AccountId");
        }
        mValue = value;
    }

    @JsonValue
    public String getValue() {
        return mValue;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AccountId identifier && mValue.equals(identifier.mValue);
    }

    @Override
    public int hashCode() {
        return mValue.hashCode();
    }
}
