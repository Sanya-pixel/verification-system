package com.warehouse.verification.controller;

import com.warehouse.verification.model.UserAccount;
import com.warehouse.verification.repository.UserAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@CrossOrigin(origins = "*")
public class UserManagementController {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder encoder;

    public UserManagementController(UserAccountRepository ur, PasswordEncoder pe) {
        this.userRepository = ur;
        this.encoder = pe;
    }

    // Create new user — ADMIN only (enforced by SecurityConfig)
    @PostMapping("/create")
    public ResponseEntity<String> registerNewWarehouseStaff(
            @RequestBody UserAccount userContract) {

        if (userRepository.existsByUsername(userContract.getUsername())) {
            return ResponseEntity.badRequest()
                    .body("Conflict: Username already taken.");
        }

        userContract.setPassword(encoder.encode(userContract.getPassword()));
        userRepository.save(userContract);
        return ResponseEntity.ok("User registered successfully.");
    }

    // Get all users — ADMIN only
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }
}