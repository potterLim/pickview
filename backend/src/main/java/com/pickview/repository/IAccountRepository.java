package com.pickview.repository;

import com.pickview.model.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IAccountRepository extends JpaRepository<Account, String> {
    @Query("select account from Account account where account.mEmail = :email")
    Optional<Account> findByEmail(@Param("email") String email);
}
