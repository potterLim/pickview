package com.pickview.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "order_lines")
public class OrderLine {
    @Id
    @Column(name = "id", length = 64)
    private String mId;

    @Version
    @Column(name = "version")
    private long mVersion;

    @Column(name = "purchase_id", nullable = false, length = 12000)
    private String mPurchaseId;

    @Column(name = "buyer_id", nullable = false, length = 12000)
    private String mBuyerId;

    @Column(name = "seller_id", nullable = false, length = 12000)
    private String mSellerId;

    @Column(name = "product_id", nullable = false, length = 12000)
    private String mProductId;

    @Column(name = "title", nullable = false, length = 12000)
    private String mTitle;

    @Column(name = "price_won", nullable = false)
    private int mPriceWon;

    @Column(name = "channel_fee_won", nullable = false)
    private int mChannelFeeWon;

    @Column(name = "platform_fee_won", nullable = false)
    private int mPlatformFeeWon;

    @Column(name = "seller_amount_won", nullable = false)
    private int mSellerAmountWon;

    @Column(name = "term_days", nullable = false)
    private int mTermDays;

    @Column(name = "refunded", nullable = false)
    private boolean mIsRefunded;

    @Column(name = "settlement_id", nullable = false, length = 12000)
    private String mSettlementId;

    protected OrderLine() {
        // Required by JPA; application code uses the complete constructor.
    }

    public OrderLine(String id, String purchaseId, String buyerId, String sellerId, String productId, String title, int priceWon, int channelFeeWon, int platformFeeWon, int sellerAmountWon, int termDays, boolean refunded, String settlementId) {
        mId = id;
        mPurchaseId = purchaseId;
        mBuyerId = buyerId;
        mSellerId = sellerId;
        mProductId = productId;
        mTitle = title;
        mPriceWon = priceWon;
        mChannelFeeWon = channelFeeWon;
        mPlatformFeeWon = platformFeeWon;
        mSellerAmountWon = sellerAmountWon;
        mTermDays = termDays;
        mIsRefunded = refunded;
        mSettlementId = settlementId;
    }

    public String getId() { return mId; }
    public String getPurchaseId() { return mPurchaseId; }
    public String getBuyerId() { return mBuyerId; }
    public String getSellerId() { return mSellerId; }
    public String getProductId() { return mProductId; }
    public String getTitle() { return mTitle; }
    public int getPriceWon() { return mPriceWon; }
    public int getChannelFeeWon() { return mChannelFeeWon; }
    public int getPlatformFeeWon() { return mPlatformFeeWon; }
    public int getSellerAmountWon() { return mSellerAmountWon; }
    public int getTermDays() { return mTermDays; }
    public boolean isRefunded() { return mIsRefunded; }
    public String getSettlementId() { return mSettlementId; }

    public void refund() { mIsRefunded = true; }
    public void settle(String settlementId) { mSettlementId = settlementId; }
}
