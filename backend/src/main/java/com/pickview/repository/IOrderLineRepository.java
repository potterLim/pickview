package com.pickview.repository;

import com.pickview.model.OrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IOrderLineRepository extends JpaRepository<OrderLine, String> {

}
