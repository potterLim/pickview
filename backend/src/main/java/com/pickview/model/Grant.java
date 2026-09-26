package com.pickview.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "grants")
public class Grant {

    @Id
    @Column(name = "id", length = 64)
    private String mId;

    @Version
    @Column(name = "version")
    private long mVersion;

    @Column(name = "buyer_id", nullable = false, length = 12000)
    private String mBuyerId;

    @Column(name = "product_id", nullable = false, length = 12000)
    private String mProductId;

    @Column(name = "line_id", nullable = false, length = 12000)
    private String mLineId;

    @Column(name = "expires_at", nullable = false)
    private long mExpiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean mIsRevoked;

    protected Grant() {
        // Required by JPA; application code uses the complete constructor.
    }

    public Grant(String id, String buyerId, String productId, String lineId, long expiresAt, boolean revoked) {
        mId = id;
        mBuyerId = buyerId;
        mProductId = productId;
        mLineId = lineId;
        mExpiresAt = expiresAt;
        mIsRevoked = revoked;
    }

    public String getId() {
        return mId;
    }

    public String getBuyerId() {
        return mBuyerId;
    }

    public String getProductId() {
        return mProductId;
    }

    public String getLineId() {
        return mLineId;
    }

    public long getExpiresAt() {
        return mExpiresAt;
    }

    public boolean isRevoked() {
        return mIsRevoked;
    }

    public boolean isValid(long now) {
        return !mIsRevoked && (mExpiresAt == 0 || mExpiresAt > now);
    }

    public void revoke() {
        mIsRevoked = true;
    }
}
