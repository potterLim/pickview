package com.pickview.repository;

import com.pickview.model.Audit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IAuditRepository extends JpaRepository<Audit, String> {

}
