package com.pickview.api;

import com.pickview.model.Account;
import com.pickview.domain.Password;
import com.pickview.security.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AccountService mAccounts;

    public AuthController(AccountService accounts) {
        mAccounts = accounts;
    }

    @GetMapping("/api/health")
    public Map<String, String> getHealth() {
        return Map.of("status", "ok", "application", "PickView");
    }

    @PostMapping("/api/auth/login")
    public Map<String, String> login(@Valid @RequestBody LoginRequest request) {
        return Map.of("token", mAccounts.login(request.email(), new Password(request.password())));
    }

    @PostMapping("/api/auth/register")
    public Map<String, String> register(@Valid @RequestBody RegisterRequest request) {
        mAccounts.register(request.email(), new Password(request.password()), request.name(), request.isAdult());
        return Map.of("token", mAccounts.login(request.email(), new Password(request.password())));
    }

    @PostMapping("/api/auth/logout")
    public void logout(@RequestHeader("Authorization") String authorization) {
        mAccounts.logout(authorization.substring(7));
    }

    @GetMapping("/api/me")
    public UserView getCurrentUser(Principal principal) {
        return UserView.fromAccount(mAccounts.requireAccount(principal.getName()));
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank @Size(max = 64) String password) {}

    public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(max = 64) String password,
        @NotBlank @Size(max = 80) String name,
        boolean isAdult
    ) {}

    public record UserView(
        String id,
        String email,
        String displayName,
        String role,
        String sellerStatus,
        String bio,
        String language,
        String interests
    ) {
        public static UserView fromAccount(Account account) {
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
}
