package com.warehouse.verification.service;
import com.warehouse.verification.model.UserAccount;
import com.warehouse.verification.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserAccountRepository userRepository;
    public CustomUserDetailsService(UserAccountRepository ur) {
        this.userRepository = ur;
    }
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Fetch matching profile details from the user accounts tracking table
        UserAccount account = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Staff profile username not found: " +
                        username));
        // Maps string tags cleanly into standardized internal role authorities
        return User.builder()
                .username(account.getUsername())
                .password(account.getPassword()) // Enforces evaluation of BCrypt hashed text strings
                .roles(account.getRoles().stream().map(role -> role.replace("ROLE_",
                        "")).toArray(String[]::new))
                .build();
    }
}
