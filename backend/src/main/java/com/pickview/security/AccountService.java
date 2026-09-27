package com.pickview.security;

import com.pickview.api.ApiFailure;
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
import java.util.Locale;
import java.util.UUID;
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

    public Account requireAccount(String id) {
        return mAccounts
            .findById(id)
            .orElseThrow(() -> new ApiFailure(401, "로그인이 필요합니다. / Sign in required."));
    }

    @Transactional
    public String login(String email, Password password) {
        Account account = mAccounts
            .findByEmail(email.strip().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new ApiFailure(401, "이메일 또는 비밀번호를 확인해 주세요. / Invalid credentials."));
        if (!password.matches(mEncoder, account.getPasswordHash())) {
            throw new ApiFailure(401, "이메일 또는 비밀번호를 확인해 주세요. / Invalid credentials.");
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
    public Account register(String email, Password password, String name, boolean isAdult) {
        if (!isAdult) {
            throw new ApiFailure(
                400,
                "성인 확인과 10~64자 비밀번호가 필요합니다. / Adult confirmation and 10–64 character password required."
            );
        }
        return mAccounts.save(
            new Account(
                UUID.randomUUID().toString(),
                email.strip().toLowerCase(Locale.ROOT),
                password.encode(mEncoder),
                name,
                com.pickview.domain.ERole.BUYER,
                com.pickview.domain.ESellerStatus.NONE,
                "",
                "ko",
                ""
            )
        );
    }

    public Account authenticateOrNull(String token) {
        String digest = hashToken(token);
        return mSessions
            .findAll()
            .stream()
            .filter(
                session -> session.getTokenHash().equals(digest) && session.getExpiresAt() > System.currentTimeMillis()
            )
            .findFirst()
            .flatMap(session -> mAccounts.findById(session.getUserId()))
            .orElse(null);
    }

    @Transactional
    public void logout(String token) {
        String digest = hashToken(token);
        mSessions.deleteAll(
            mSessions
                .findAll()
                .stream()
                .filter(session -> session.getTokenHash().equals(digest))
                .toList()
        );
    }

    private String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 required", failure);
        }
    }
}
