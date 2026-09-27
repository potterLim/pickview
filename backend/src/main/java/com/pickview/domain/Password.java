package com.pickview.domain;

import com.pickview.api.ApiFailure;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class Password {

    private static final int MIN_CHARACTERS = 10;
    private static final int MAX_CHARACTERS = 64;
    private static final int MAX_UTF8_BYTES = 72;

    private final String mValue;

    public Password(String value) {
        if (value == null
            || value.length() < MIN_CHARACTERS
            || value.length() > MAX_CHARACTERS
            || value.getBytes(StandardCharsets.UTF_8).length > MAX_UTF8_BYTES) {
            throw new ApiFailure(HttpStatus.BAD_REQUEST, "비밀번호는 10~64자, UTF-8 72바이트 이하여야 합니다. / Password must be 10–64 characters and at most 72 UTF-8 bytes.");
        }
        mValue = value;
    }

    public String encode(PasswordEncoder encoder) {
        return encoder.encode(mValue);
    }

    public boolean matches(PasswordEncoder encoder, String encodedPassword) {
        return encoder.matches(mValue, encodedPassword);
    }
}
