package com.pickview.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "tickets")
public class Ticket {

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

    @Column(name = "recipient_id", nullable = false, length = 12000)
    private String mRecipientId;

    @Column(name = "kind", nullable = false, length = 12000)
    private String mKind;

    @Column(name = "message", nullable = false, length = 12000)
    private String mMessage;

    @Column(name = "status", nullable = false, length = 12000)
    private String mStatus;

    @Column(name = "reply", nullable = false, length = 12000)
    private String mReply;

    @Column(name = "created_at", nullable = false)
    private long mCreatedAt;

    protected Ticket() {
        // Required by JPA; application code uses the complete constructor.
    }

    public Ticket(
        String id,
        String userId,
        String targetId,
        String recipientId,
        String kind,
        String message,
        String status,
        String reply,
        long createdAt
    ) {
        mId = id;
        mUserId = userId;
        mTargetId = targetId;
        mRecipientId = recipientId;
        mKind = kind;
        mMessage = message;
        mStatus = status;
        mReply = reply;
        mCreatedAt = createdAt;
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

    public String getRecipientId() {
        return mRecipientId;
    }

    public String getKind() {
        return mKind;
    }

    public String getMessage() {
        return mMessage;
    }

    public String getStatus() {
        return mStatus;
    }

    public String getReply() {
        return mReply;
    }

    public long getCreatedAt() {
        return mCreatedAt;
    }

    public void resolve(String reply, String status) {
        mReply = reply;
        mStatus = status;
    }
}
