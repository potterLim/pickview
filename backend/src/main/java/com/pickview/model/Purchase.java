package com.pickview.model;

import com.pickview.domain.EPaymentChannel;

import com.pickview.domain.EOrderStatus;

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

    public Purchase(String id, String buyerId, String requestKey, EOrderStatus status, EPaymentChannel channel, long createdAt) {
        mId = id;
        mBuyerId = buyerId;
        mRequestKey = requestKey;
        mStatus = status.name();
        mChannel = channel.name();
        mCreatedAt = createdAt;
    }

    public String getId() {
        return mId;
    }

    public String getBuyerId() {
        return mBuyerId;
    }

    public String getRequestKey() {
        return mRequestKey;
    }

    public EOrderStatus getStatus() {
        return EOrderStatus.valueOf(mStatus);
    }

    public EPaymentChannel getChannel() {
        return EPaymentChannel.valueOf(mChannel);
    }

    public long getCreatedAt() {
        return mCreatedAt;
    }

    public void refund() {
        mStatus = EOrderStatus.REFUNDED.name();
    }
}
