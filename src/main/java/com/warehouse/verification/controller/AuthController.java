package com.warehouse.verification.controller;

import com.warehouse.verification.dto.LoginRequest;
import com.warehouse.verification.dto.RegisterRequest;
import com.warehouse.verification.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ── POST /api/v1/auth/login ──────────────────────────────────────────────
    // Open to everyone — no token needed
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        try {
            Map<String, Object> response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid username or password"));
        }
    }

    // ── POST /api/v1/auth/register ───────────────────────────────────────────
    // ADMIN only — SecurityConfig + @PreAuthorize double protection
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        try {
            Map<String, Object> response = authService.register(request);
            return ResponseEntity.status(201).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/v1/auth/me ──────────────────────────────────────────────────
    // Returns current logged-in user info from JWT
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(
            @RequestHeader("Authorization") String authHeader) {
        // JWT filter already validated token — just return context info
        // In real implementation extract from SecurityContextHolder
        return ResponseEntity.ok(Map.of("message", "Token is valid"));
    }
}