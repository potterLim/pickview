package com.pickview.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.pickview.api.ApiFailure;
import org.springframework.http.HttpStatus;

public final class OrderLineId {

    private final String mValue;

    @JsonCreator
    public OrderLineId(String value) {
        if (value == null || value.isBlank() || value.length() > 64) {
            throw new ApiFailure(HttpStatus.BAD_REQUEST, "Invalid OrderLineId");
        }
        mValue = value;
    }

    @JsonValue
    public String getValue() {
        return mValue;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OrderLineId identifier && mValue.equals(identifier.mValue);
    }

    @Override
    public int hashCode() {
        return mValue.hashCode();
    }
}
