package com.pickview.model;

import com.pickview.domain.EActivityKind;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "engagements")
public class Engagement {

    @Id
    @Column(name = "id", length = 64)
    private String mId;

    @Version
    @Column(name = "version")
    private long mVersion;

    @Column(name = "user_id", nullable = false, length = 12000)
    private String mUserId;

    @Column(name = "target_id", nullable = false, length = 12000)
    private String mTargetId;

    @Column(name = "kind", nullable = false, length = 12000)
    private String mKind;

    @Column(name = "content", nullable = false, length = 12000)
    private String mContent;

    @Column(name = "number_value", nullable = false)
    private double mNumberValue;

    @Column(name = "updated_at", nullable = false)
    private long mUpdatedAt;

    protected Engagement() {
        // Required by JPA; application code uses the complete constructor.
    }

    public Engagement(
        String id,
        String userId,
        String targetId,
        EActivityKind kind,
        String content,
        double numberValue,
        long updatedAt
    ) {
        mId = id;
        mUserId = userId;
        mTargetId = targetId;
        mKind = kind.name();
        mContent = content;
        mNumberValue = numberValue;
        mUpdatedAt = updatedAt;
    }

    public String getId() {
        return mId;
    }

    public String getUserId() {
        return mUserId;
    }

    public String getTargetId() {
        return mTargetId;
    }

    public EActivityKind getKind() {
        return EActivityKind.valueOf(mKind);
    }

    public String getContent() {
        return mContent;
    }

    public double getNumberValue() {
        return mNumberValue;
    }

    public long getUpdatedAt() {
        return mUpdatedAt;
    }

    public void revise(String content, double numberValue, long now) {
        mContent = content;
        mNumberValue = numberValue;
        mUpdatedAt = now;
    }
}
