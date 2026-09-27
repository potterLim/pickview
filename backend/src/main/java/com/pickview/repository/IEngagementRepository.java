package com.pickview.repository;

import com.pickview.model.Engagement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface IEngagementRepository extends JpaRepository<Engagement, String> {

    @Query("select item.mTargetId as productId, avg(item.mNumberValue) as rating, count(item) as reviewCount "
        + "from Engagement item where item.mKind = 'REVIEW' and item.mTargetId in :ids group by item.mTargetId")
    List<IProductRating> summarizeReviews(@Param("ids") List<String> ids);
}
