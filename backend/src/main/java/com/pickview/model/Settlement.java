package com.pickview.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "settlements")
public class Settlement {
    @Id
    @Column(name = "id", length = 64)
    private String mId;

    @Version
    @Column(name = "version")
    private long mVersion;

    @Column(name = "seller_id", nullable = false, length = 12000)
    private String mSellerId;

    @Column(name = "amount_won", nullable = false)
    private int mAmountWon;

    @Column(name = "created_at", nullable = false)
    private long mCreatedAt;

    protected Settlement() {
        // Required by JPA; application code uses the complete constructor.
    }

    public Settlement(String id, String sellerId, int amountWon, long createdAt) {
        mId = id;
        mSellerId = sellerId;
        mAmountWon = amountWon;
        mCreatedAt = createdAt;
    }

    public String getId() { return mId; }
    public String getSellerId() { return mSellerId; }
    public int getAmountWon() { return mAmountWon; }
    public long getCreatedAt() { return mCreatedAt; }
}
