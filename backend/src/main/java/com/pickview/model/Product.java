package com.pickview.model;

import com.pickview.domain.EProductKind;

import com.pickview.domain.EProductStatus;

import com.pickview.domain.ECategory;

import com.pickview.domain.EAccessTerm;
import com.pickview.domain.WonAmount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(name = "id", length = 64)
    private String mId;

    @Version
    @Column(name = "version")
    private long mVersion;

    @Column(name = "seller_id", nullable = false, length = 12000)
    private String mSellerId;

    @Column(name = "title", nullable = false, length = 12000)
    private String mTitle;

    @Column(name = "description", nullable = false, length = 12000)
    private String mDescription;

    @Column(name = "category", nullable = false, length = 12000)
    private String mCategory;

    @Column(name = "price_won", nullable = false)
    private int mPriceWon;

    @Column(name = "term_days", nullable = false)
    private int mTermDays;

    @Column(name = "status", nullable = false, length = 12000)
    private String mStatus;

    @Column(name = "thumbnail", nullable = false, length = 12000)
    private String mThumbnail;

    @Column(name = "tags", nullable = false, length = 300)
    private String mTags = "";

    @Column(name = "media_key", nullable = false, length = 12000)
    private String mMediaKey;

    @Column(name = "preview_key", nullable = false, length = 12000)
    private String mPreviewKey;

    @Column(name = "duration_seconds", nullable = false)
    private double mDurationSeconds;

    @Column(name = "kind", nullable = false, length = 12000)
    private String mKind;

    @Column(name = "bundle_ids", nullable = false, length = 12000)
    private String mBundleIds;

    @Column(name = "blocked", nullable = false)
    private boolean mIsBlocked;

    @Column(name = "created_at", nullable = false)
    private long mCreatedAt;

    protected Product() {
        // Required by JPA; application code uses the complete constructor.
    }

    public Product(
        String id,
        String sellerId,
        String title,
        String description,
        ECategory category,
        int priceWon,
        int termDays,
        EProductStatus status,
        String thumbnail,
        String mediaKey,
        String previewKey,
        double durationSeconds,
        EProductKind kind,
        String bundleIds,
        boolean blocked,
        long createdAt
    ) {
        mId = id;
        mSellerId = sellerId;
        mTitle = title;
        mDescription = description;
        mCategory = category.name();
        mPriceWon = priceWon;
        mTermDays = termDays;
        mStatus = status.name();
        mThumbnail = thumbnail;
        mMediaKey = mediaKey;
        mPreviewKey = previewKey;
        mDurationSeconds = durationSeconds;
        mKind = kind.name();
        mBundleIds = bundleIds;
        mIsBlocked = blocked;
        mCreatedAt = createdAt;
    }

    public String getId() {
        return mId;
    }

    public String getSellerId() {
        return mSellerId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public ECategory getCategory() {
        return ECategory.valueOf(mCategory);
    }

    public int getPriceWon() {
        return mPriceWon;
    }

    public int getTermDays() {
        return mTermDays;
    }

    public EProductStatus getStatus() {
        return EProductStatus.valueOf(mStatus);
    }

    public String getThumbnail() {
        return mThumbnail;
    }

    public String getTags() {
        return mTags;
    }

    public void changeTags(String tags) {
        mTags = tags;
    }

    public String getMediaKey() {
        return mMediaKey;
    }

    public String getPreviewKey() {
        return mPreviewKey;
    }

    public double getDurationSeconds() {
        return mDurationSeconds;
    }

    public EProductKind getKind() {
        return EProductKind.valueOf(mKind);
    }

    public String getBundleIds() {
        return mBundleIds;
    }

    public boolean isBlocked() {
        return mIsBlocked;
    }

    public long getCreatedAt() {
        return mCreatedAt;
    }

    public void publish() {
        mStatus = EProductStatus.APPROVED.name();
    }

    public void reject() {
        mStatus = EProductStatus.REJECTED.name();
    }

    public void withdraw() {
        mStatus = EProductStatus.WITHDRAWN.name();
    }

    public void submit() {
        mStatus = EProductStatus.PENDING.name();
    }

    public void block() {
        mIsBlocked = true;
    }

    public void revise(String title, String description, WonAmount price, EAccessTerm term) {
        mTitle = title;
        mDescription = description;
        mPriceWon = price.getWon();
        mTermDays = term.getDays();
    }

    public void changePresentation(ECategory category, String thumbnail) {
        mCategory = category.name();
        mThumbnail = thumbnail;
    }

    public void replaceMedia(String mediaKey, String previewKey, double durationSeconds) {
        mMediaKey = mediaKey;
        mPreviewKey = previewKey;
        mDurationSeconds = durationSeconds;
        mStatus = EProductStatus.PENDING.name();
    }
}
