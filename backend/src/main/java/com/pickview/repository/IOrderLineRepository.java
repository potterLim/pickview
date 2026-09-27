package com.pickview.repository;

import com.pickview.model.OrderLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IOrderLineRepository extends JpaRepository<OrderLine, String> {
    @Query(
        "select line.mProductId as productId, count(line) as sales from OrderLine line " +
            "where line.mProductId in :ids and line.mIsRefunded = false group by line.mProductId"
    )
    List<IProductSales> summarizeSales(@Param("ids") List<String> ids);
}
