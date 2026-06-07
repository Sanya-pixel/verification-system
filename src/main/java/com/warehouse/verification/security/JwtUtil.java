package com.warehouse.verification.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // 256-bit secret — in production store in application.properties / Vault
    private static final String SECRET = "warehouse-verification-super-secret-key-2024!!";
    private static final long   EXPIRY_MS = 24 * 60 * 60 * 1000L; // 24 hours

    private final javax.crypto.SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());

    // ── Generate token ──────────────────────────────────────────────────────
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRY_MS))
                .signWith(key)
                .compact();
    }

    // ── Extract claims ───────────────────────────────────────────────────────
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return (String) parseClaims(token).get("role");
    }

    // ── Validate ─────────────────────────────────────────────────────────────
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ── Private helper ───────────────────────────────────────────────────────
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}