package com.pickview.api;

import com.pickview.domain.ERole;
import com.pickview.domain.ESellerStatus;
import com.pickview.model.Account;

public record UserView(
    String id,
    String email,
    String displayName,
    ERole role,
    ESellerStatus sellerStatus,
    String bio,
    String language,
    String interests
) {
    public static UserView createFromAccount(Account account) {
        return new UserView(
            account.getId(),
            account.getEmail(),
            account.getDisplayName(),
            account.getRole(),
            account.getSellerStatus(),
            account.getBio(),
            account.getLanguage(),
            account.getInterests()
        );
    }
}
