package com.pickview.repository;

import com.pickview.model.RefundAdjustment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IRefundAdjustmentRepository extends JpaRepository<RefundAdjustment, String> {
    @Query("select a from RefundAdjustment a where a.mSellerId = :sellerId and a.mSettlementId = :settlementId")
    List<RefundAdjustment> findPending(String sellerId, String settlementId);
}
