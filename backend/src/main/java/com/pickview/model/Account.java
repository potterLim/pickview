package com.pickview.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @Column(name = "id", length = 64)
    private String mId;

    @Version
    @Column(name = "version")
    private long mVersion;

    @Column(name = "email", nullable = false, length = 12000)
    private String mEmail;

    @Column(name = "password_hash", nullable = false, length = 12000)
    private String mPasswordHash;

    @Column(name = "display_name", nullable = false, length = 12000)
    private String mDisplayName;

    @Column(name = "role", nullable = false, length = 12000)
    private String mRole;

    @Column(name = "seller_status", nullable = false, length = 12000)
    private String mSellerStatus;

    @Column(name = "bio", nullable = false, length = 12000)
    private String mBio;

    @Column(name = "language", nullable = false, length = 12000)
    private String mLanguage;

    @Column(name = "interests", nullable = false, length = 12000)
    private String mInterests;

    protected Account() {
        // Required by JPA; application code uses the complete constructor.
    }

    public Account(
        String id,
        String email,
        String passwordHash,
        String displayName,
        String role,
        String sellerStatus,
        String bio,
        String language,
        String interests
    ) {
        mId = id;
        mEmail = email;
        mPasswordHash = passwordHash;
        mDisplayName = displayName;
        mRole = role;
        mSellerStatus = sellerStatus;
        mBio = bio;
        mLanguage = language;
        mInterests = interests;
    }

    public String getId() {
        return mId;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getPasswordHash() {
        return mPasswordHash;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public String getRole() {
        return mRole;
    }

    public String getSellerStatus() {
        return mSellerStatus;
    }

    public String getBio() {
        return mBio;
    }

    public String getLanguage() {
        return mLanguage;
    }

    public String getInterests() {
        return mInterests;
    }

    public void approveSeller() {
        mSellerStatus = "APPROVED";
    }

    public void applySeller(String displayName, String bio) {
        mDisplayName = displayName;
        mBio = bio;
        mSellerStatus = "PENDING";
    }

    public void rejectSeller() {
        mSellerStatus = "REJECTED";
    }

    public void changeSettings(String language, String interests) {
        mLanguage = language;
        mInterests = interests;
    }

    public void changeRole(String role) {
        mRole = role;
    }

    public void changeProfile(String displayName, String bio) {
        mDisplayName = displayName;
        mBio = bio;
    }
}
