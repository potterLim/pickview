package com.pickview.repository;

import com.pickview.model.Grant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IGrantRepository extends JpaRepository<Grant, String> {
    @Query(
        "select count(grant) > 0 from Grant grant where grant.mBuyerId = :buyerId and grant.mProductId = :productId " +
            "and grant.mIsRevoked = false and (grant.mExpiresAt = 0 or grant.mExpiresAt > :now)"
    )
    boolean hasValidGrant(@Param("buyerId") String buyerId, @Param("productId") String productId, @Param("now") long now);
}
