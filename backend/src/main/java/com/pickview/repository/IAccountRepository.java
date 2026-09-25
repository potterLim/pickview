package com.pickview.repository;

import com.pickview.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IAccountRepository extends JpaRepository<Account, String> {
    java.util.Optional<Account> findBymEmail(String email);
}
