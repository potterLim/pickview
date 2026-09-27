package com.pickview.repository;

import com.pickview.model.LoginSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ILoginSessionRepository extends JpaRepository<LoginSession, String> {
    @Query("select session from LoginSession session where session.mTokenHash = :digest and session.mExpiresAt > :now")
    Optional<LoginSession> findValidSession(@Param("digest") String digest, @Param("now") long now);

    @Modifying
    @Query("delete from LoginSession session where session.mTokenHash = :digest")
    void deleteToken(@Param("digest") String digest);
}
