package com.warehouse.verification.config;

import com.warehouse.verification.model.UserAccount;
import com.warehouse.verification.repository.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration
        .EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final UserAccountRepository userAccountRepository;

    public SecurityConfig(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            UserAccount account = userAccountRepository
                    .findByUsername(username)
                    .orElseThrow(() ->
                            new UsernameNotFoundException("User not found: " + username));

            return new org.springframework.security.core.userdetails.User(
                    account.getUsername(),
                    account.getPassword(),
                    account.getRoles().stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList())
            );
        };
    }

    @Bean
    public SecurityFilterChain isolationFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public — login
                        .requestMatchers("/api/v1/auth/login").permitAll()

                        // ADMIN only
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/warehouse/inventory/bulk-upload").hasRole("ADMIN")
                        .requestMatchers("/api/v1/warehouse/reports").hasRole("ADMIN")

                        // OPERATOR + ADMIN
                        .requestMatchers("/api/v1/warehouse/verify/**").hasAnyRole("ADMIN","OPERATOR")
                        .requestMatchers("/api/v1/pod/**").hasAnyRole("ADMIN","OPERATOR")

                        // Everything else needs login
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> {}); // Basic auth for simplicity
        return http.build();
    }
}