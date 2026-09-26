package com.pickview.repository;

import com.pickview.model.Grant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IGrantRepository extends JpaRepository<Grant, String> {}
