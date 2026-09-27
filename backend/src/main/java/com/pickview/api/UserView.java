package com.pickview.api;

import com.pickview.model.Account;

public record UserView(
    String id,
    String email,
    String displayName,
    com.pickview.domain.ERole role,
    com.pickview.domain.ESellerStatus sellerStatus,
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
