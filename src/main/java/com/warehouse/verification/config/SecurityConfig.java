package com.warehouse.verification.config;

import com.warehouse.verification.security.JwtAuthFilter;
import com.warehouse.verification.security.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // enables @PreAuthorize on controllers
public class SecurityConfig {

    private final JwtAuthFilter          jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          UserDetailsServiceImpl userDetailsService) {
        this.jwtAuthFilter   = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    // ── Security filter chain ────────────────────────────────────────────────
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authenticationProvider(authenticationProvider())

                // Stateless — no HTTP sessions, JWT handles auth
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // ── Public endpoints (no token needed) ──────────────────────
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/register").permitAll()

                        // ── ADMIN only ───────────────────────────────────────────────
                        // Upload CSV — only Admin can bulk import inventory
                        .requestMatchers("/api/v1/warehouse/inventory/bulk-upload").hasRole("ADMIN")

                        // Download QA reports — only Admin
                        .requestMatchers("/api/v1/warehouse/reports").hasRole("ADMIN")

                        // ── ADMIN + OPERATOR ─────────────────────────────────────────
                        // Verify a product by WID — both roles can scan
                        .requestMatchers("/api/v1/warehouse/verify/**").hasAnyRole("ADMIN", "OPERATOR")

                        // Proof of Delivery photo upload — both roles
                        .requestMatchers("/api/v1/pod/**").hasAnyRole("ADMIN", "OPERATOR")

                        // Who am I endpoint
                        .requestMatchers("/api/v1/auth/me").authenticated()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // Add JWT filter BEFORE Spring's username/password filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ── Password encoder (BCrypt — industry standard) ────────────────────────
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ── Authentication provider ──────────────────────────────────────────────
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    // ── Authentication manager ───────────────────────────────────────────────
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
//package com.warehouse.verification.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.web.SecurityFilterChain;
//
//@Configuration
//@EnableWebSecurity
//public class SecurityConfig {
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
//                .authorizeHttpRequests(auth -> auth
//                        .anyRequest().permitAll()  // Open for demo testing
//                );
//        return http.build();
//    }
//}