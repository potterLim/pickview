package com.pickview.repository;

import com.pickview.model.LoginSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ILoginSessionRepository extends JpaRepository<LoginSession, String> {}
