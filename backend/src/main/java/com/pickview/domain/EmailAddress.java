package com.pickview.domain;

import com.pickview.api.ApiFailure;
import java.util.Locale;
import java.util.regex.Pattern;

public final class EmailAddress {

    private static final int MAX_CHARACTERS = 254;
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
    private final String mValue;

    public EmailAddress(String value) {
        if (value == null) {
            throw new ApiFailure(400, "Email required");
        }
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        if (normalized.length() > MAX_CHARACTERS || !ADDRESS_PATTERN.matcher(normalized).matches()) {
            throw new ApiFailure(400, "Invalid email address");
        }
        mValue = normalized;
    }

    public String getValue() {
        return mValue;
    }
}
