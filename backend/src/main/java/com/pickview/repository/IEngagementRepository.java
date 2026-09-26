package com.pickview.repository;

import com.pickview.model.Engagement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IEngagementRepository extends JpaRepository<Engagement, String> {}
