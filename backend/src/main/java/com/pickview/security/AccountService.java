package com.pickview.security;

import com.pickview.api.ApiFailure;
import com.pickview.domain.AccountId;
import com.pickview.domain.ERole;
import com.pickview.domain.ESellerStatus;
import com.pickview.domain.EmailAddress;
import com.pickview.domain.Password;
import com.pickview.model.Account;
import com.pickview.model.LoginSession;
import com.pickview.repository.IAccountRepository;
import com.pickview.repository.ILoginSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private static final long SESSION_TTL_MILLIS = Duration.ofDays(7).toMillis();

    private final IAccountRepository mAccounts;
    private final ILoginSessionRepository mSessions;
    private final PasswordEncoder mEncoder;

    public AccountService(IAccountRepository accounts, ILoginSessionRepository sessions, PasswordEncoder encoder) {
        mAccounts = accounts;
        mSessions = sessions;
        mEncoder = encoder;
    }

    public Account requireAccount(AccountId id) {
        return mAccounts.findById(id.getValue()).orElseThrow(() -> new ApiFailure(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다. / Sign in required."));
    }

    @Transactional
    public String login(EmailAddress email, Password password) {
        Account account = mAccounts.findByEmail(email.getValue()).orElseThrow(() -> new ApiFailure(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호를 확인해 주세요. / Invalid credentials."));
        if (!password.matches(mEncoder, account.getPasswordHash())) {
            throw new ApiFailure(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호를 확인해 주세요. / Invalid credentials.");
        }
        String token = UUID.randomUUID().toString() + UUID.randomUUID();
        mSessions.save(
            new LoginSession(
                UUID.randomUUID().toString(),
                account.getId(),
                hashToken(token),
                System.currentTimeMillis() + SESSION_TTL_MILLIS
            )
        );
        return token;
    }

    @Transactional
    public Account register(EmailAddress email, Password password, String name, boolean isAdult) {
        if (!isAdult) {
            throw new ApiFailure(HttpStatus.BAD_REQUEST, "성인 확인과 10~64자 비밀번호가 필요합니다. / Adult confirmation and 10–64 character password required.");
        }
        return mAccounts.save(
            new Account(
                UUID.randomUUID().toString(),
                email.getValue(),
                password.encode(mEncoder),
                name,
                ERole.BUYER,
                ESellerStatus.NONE,
                "",
                "ko",
                ""
            )
        );
    }

    public Account authenticateOrNull(String token) {
        String digest = hashToken(token);
        return mSessions.findValidSession(digest, System.currentTimeMillis())
            .flatMap(session -> mAccounts.findById(session.getUserId()))
            .orElse(null);
    }

    @Transactional
    public void logout(String token) {
        String digest = hashToken(token);
        mSessions.deleteToken(digest);
    }

    private String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 required", failure);
        }
    }
}
