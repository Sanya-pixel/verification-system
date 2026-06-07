package com.warehouse.verification.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    public User() {}

    public User(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role     = role;
    }

    public Long   getId()       { return id; }
    public String getUsername() { return username; }
    public void   setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void   setPassword(String password) { this.password = password; }
    public Role   getRole()     { return role; }
    public void   setRole(Role role) { this.role = role; }
}