package com.pickview.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfiguration {

    private final String mCorsOrigins;

    public SecurityConfiguration(@Value("${pickview.cors-origins}") String corsOrigins) {
        mCorsOrigins = corsOrigins;
    }

    @Bean
    public PasswordEncoder createPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain createFilterChain(HttpSecurity http, BearerFilter filter) throws Exception {
        return http
            .csrf(configuration -> configuration.disable())
            .cors(configuration -> configuration.configurationSource(createCorsConfiguration()))
            .sessionManagement(configuration -> configuration.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(rules ->
                rules
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(
                        "/api/health",
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/public/**",
                        "/api/media/stream/**"
                    )
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .hasAnyRole("ADMIN", "CONTENT", "SUPPORT", "FINANCE")
                    .anyRequest()
                    .authenticated()
            )
            .exceptionHandling(configuration ->
                configuration.authenticationEntryPoint(this::rejectUnauthenticated)
            )
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    private void rejectUnauthenticated(HttpServletRequest request, HttpServletResponse response, AuthenticationException failure)
        throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"로그인이 필요합니다. / Sign in required.\"}");
    }

    @Bean
    public CorsConfigurationSource createCorsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(mCorsOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Range"));
        configuration.setExposedHeaders(List.of("Content-Range", "Accept-Ranges"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
