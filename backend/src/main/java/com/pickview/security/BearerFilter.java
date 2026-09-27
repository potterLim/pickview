package com.pickview.security;

import com.pickview.model.Account;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BearerFilter extends OncePerRequestFilter {

    private final AccountService mAccounts;

    public BearerFilter(AccountService accounts) {
        mAccounts = accounts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String authorizationOrNull = request.getHeader("Authorization");
        if (authorizationOrNull != null && authorizationOrNull.startsWith("Bearer ")) {
            Account accountOrNull = mAccounts.authenticateOrNull(authorizationOrNull.substring(7));
            if (accountOrNull != null) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    accountOrNull.getId(),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + accountOrNull.getRole()))
                ));
            }
        }
        chain.doFilter(request, response);
    }
}
