package com.pickview.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "login_sessions")
public class LoginSession {

    @Id
    @Column(name = "id", length = 64)
    private String mId;

    @Version
    @Column(name = "version")
    private long mVersion;

    @Column(name = "user_id", nullable = false, length = 12000)
    private String mUserId;

    @Column(name = "token_hash", nullable = false, length = 12000)
    private String mTokenHash;

    @Column(name = "expires_at", nullable = false)
    private long mExpiresAt;

    protected LoginSession() {
        // Required by JPA; application code uses the complete constructor.
    }

    public LoginSession(String id, String userId, String tokenHash, long expiresAt) {
        mId = id;
        mUserId = userId;
        mTokenHash = tokenHash;
        mExpiresAt = expiresAt;
    }

    public String getId() {
        return mId;
    }

    public String getUserId() {
        return mUserId;
    }

    public String getTokenHash() {
        return mTokenHash;
    }

    public long getExpiresAt() {
        return mExpiresAt;
    }
}
