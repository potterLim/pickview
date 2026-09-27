package com.pickview.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.pickview.api.ApiFailure;

public final class TicketId {

    private final String mValue;

    @JsonCreator
    public TicketId(String value) {
        if (value == null || value.isBlank() || value.length() > 64) {
            throw new ApiFailure(400, "Invalid TicketId");
        }
        mValue = value;
    }

    @JsonValue
    public String getValue() {
        return mValue;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TicketId identifier && mValue.equals(identifier.mValue);
    }

    @Override
    public int hashCode() {
        return mValue.hashCode();
    }
}
