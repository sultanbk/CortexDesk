package com.prodapt.network_ticketing.controller;

import com.prodapt.network_ticketing.entity.User;
import com.prodapt.network_ticketing.repository.UserRepository;
import com.prodapt.network_ticketing.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth") // Standard practice to use /api/auth
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder; // Add this!

    public AuthController(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String plainPassword = request.get("password");

        // 1. Find the user
        User user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid Username"));
        }

        // 2. BCrypt Comparison
        // .matches(plainText, encodedHash)
        if (!passwordEncoder.matches(plainPassword, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid Password"));
        }

        // 3. Safe Role Extraction
        String roleName = "USER";
        if (user.getRole() != null && user.getRole().getRoleName() != null) {
            roleName = user.getRole().getRoleName().name();
        }

        // 4. Generate Token
        String token = jwtUtil.generateToken(user.getUsername(), roleName);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", roleName,
                "username", user.getUsername(),
                "userId", user.getUserId()
        ));
    }
}
