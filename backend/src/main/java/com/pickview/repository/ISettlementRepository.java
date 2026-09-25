package com.pickview.repository;

import com.pickview.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ISettlementRepository extends JpaRepository<Settlement, String> {

}
