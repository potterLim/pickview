package com.pickview.repository;

import com.pickview.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPurchaseRepository extends JpaRepository<Purchase, String> {

}
