package com.pickview.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "audits")
public class Audit {

    @Id
    @Column(name = "id", length = 64)
    private String mId;

    @Version
    @Column(name = "version")
    private long mVersion;

    @Column(name = "actor_id", nullable = false, length = 12000)
    private String mActorId;

    @Column(name = "action", nullable = false, length = 12000)
    private String mAction;

    @Column(name = "target_id", nullable = false, length = 12000)
    private String mTargetId;

    @Column(name = "detail", nullable = false, length = 12000)
    private String mDetail;

    @Column(name = "created_at", nullable = false)
    private long mCreatedAt;

    protected Audit() {
        // Required by JPA; application code uses the complete constructor.
    }

    public Audit(String id, String actorId, String action, String targetId, String detail, long createdAt) {
        mId = id;
        mActorId = actorId;
        mAction = action;
        mTargetId = targetId;
        mDetail = detail;
        mCreatedAt = createdAt;
    }

    public String getId() {
        return mId;
    }

    public String getActorId() {
        return mActorId;
    }

    public String getAction() {
        return mAction;
    }

    public String getTargetId() {
        return mTargetId;
    }

    public String getDetail() {
        return mDetail;
    }

    public long getCreatedAt() {
        return mCreatedAt;
    }
}
