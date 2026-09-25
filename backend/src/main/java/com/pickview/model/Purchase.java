package com.pickview.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "purchases")
public class Purchase {
    @Id
    @Column(name = "id", length = 64)
    private String mId;

    @Version
    @Column(name = "version")
    private long mVersion;

    @Column(name = "buyer_id", nullable = false, length = 12000)
    private String mBuyerId;

    @Column(name = "request_key", nullable = false, length = 12000)
    private String mRequestKey;

    @Column(name = "status", nullable = false, length = 12000)
    private String mStatus;

    @Column(name = "channel", nullable = false, length = 12000)
    private String mChannel;

    @Column(name = "created_at", nullable = false)
    private long mCreatedAt;

    protected Purchase() {
        // Required by JPA; application code uses the complete constructor.
    }

    public Purchase(String id, String buyerId, String requestKey, String status, String channel, long createdAt) {
        mId = id;
        mBuyerId = buyerId;
        mRequestKey = requestKey;
        mStatus = status;
        mChannel = channel;
        mCreatedAt = createdAt;
    }

    public String getId() { return mId; }
    public String getBuyerId() { return mBuyerId; }
    public String getRequestKey() { return mRequestKey; }
    public String getStatus() { return mStatus; }
    public String getChannel() { return mChannel; }
    public long getCreatedAt() { return mCreatedAt; }

    public void refund() { mStatus = "REFUNDED"; }
}
