package com.warehouse.verification.service;

import com.warehouse.verification.dto.LoginRequest;
import com.warehouse.verification.dto.RegisterRequest;
import com.warehouse.verification.model.Role;
import com.warehouse.verification.model.User;
import com.warehouse.verification.repository.UserRepository;
import com.warehouse.verification.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository       userRepository;
    private final PasswordEncoder      passwordEncoder;
    private final JwtUtil              jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager) {
        this.userRepository       = userRepository;
        this.passwordEncoder      = passwordEncoder;
        this.jwtUtil              = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    // ── Login ────────────────────────────────────────────────────────────────
    public Map<String, Object> login(LoginRequest request) {
        try {
            System.out.println("=== LOGIN ATTEMPT ===");
            System.out.println("Username: " + request.getUsername());
            System.out.println("Password: " + request.getPassword());

            // Check if user exists first
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found in DB"));

            System.out.println("User found: " + user.getUsername());
            System.out.println("Stored hash: " + user.getPassword());

            // Check password manually
            boolean matches = passwordEncoder.matches(request.getPassword(), user.getPassword());
            System.out.println("Password matches: " + matches);

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", user.getUsername());
            response.put("role", user.getRole().name());
            response.put("message", "Login successful");
            return response;

        } catch (Exception e) {
            System.out.println("=== LOGIN FAILED ===");
            System.out.println("Error: " + e.getMessage());
            throw e;
        }
    }

    // ── Register (Admin only — enforced at SecurityConfig level) ─────────────
    public Map<String, Object> register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role. Must be ADMIN or OPERATOR");
        }

        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                role
        );

        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User created successfully");
        response.put("username", user.getUsername());
        response.put("role", user.getRole().name());
        return response;
    }
}