package com.pickview.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "notices")
public class Notice {
    @Id
    @Column(name = "id", length = 64)
    private String mId;

    @Version
    @Column(name = "version")
    private long mVersion;

    @Column(name = "user_id", nullable = false, length = 12000)
    private String mUserId;

    @Column(name = "message", nullable = false, length = 12000)
    private String mMessage;

    @Column(name = "read", nullable = false)
    private boolean mRead;

    @Column(name = "created_at", nullable = false)
    private long mCreatedAt;

    protected Notice() {
        // Required by JPA; application code uses the complete constructor.
    }

    public Notice(String id, String userId, String message, boolean read, long createdAt) {
        mId = id;
        mUserId = userId;
        mMessage = message;
        mRead = read;
        mCreatedAt = createdAt;
    }

    public String getId() { return mId; }
    public String getUserId() { return mUserId; }
    public String getMessage() { return mMessage; }
    public boolean isRead() { return mRead; }
    public long getCreatedAt() { return mCreatedAt; }

    public void markRead() { mRead = true; }
}
