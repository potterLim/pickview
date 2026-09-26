package com.pickview.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "refund_adjustments")
public class RefundAdjustment {

    @Id
    @Column(name = "line_id", length = 64)
    private String mLineId;

    @Version
    @Column(name = "version")
    private long mVersion;

    @Column(name = "seller_id", nullable = false, length = 64)
    private String mSellerId;

    @Column(name = "amount_won", nullable = false)
    private int mAmountWon;

    @Column(name = "settlement_id", nullable = false, length = 64)
    private String mSettlementId;

    protected RefundAdjustment() {
        // JPA restores the persisted adjustment.
    }

    public RefundAdjustment(String lineId, String sellerId, int amountWon) {
        mLineId = lineId;
        mSellerId = sellerId;
        mAmountWon = amountWon;
        mSettlementId = "";
    }

    public String getLineId() {
        return mLineId;
    }

    public String getSellerId() {
        return mSellerId;
    }

    public int getAmountWon() {
        return mAmountWon;
    }

    public String getSettlementId() {
        return mSettlementId;
    }

    public void settle(String settlementId) {
        mSettlementId = settlementId;
    }
}
