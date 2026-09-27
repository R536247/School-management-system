package com.schoolms.controller;

import com.schoolms.entity.RefreshToken;
import com.schoolms.entity.User;
import com.schoolms.repository.RefreshTokenRepository;
import com.schoolms.repository.UserRepository;
import com.schoolms.security.JwtUtil;
import com.schoolms.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshRepo;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, RefreshTokenRepository refreshRepo) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshRepo = refreshRepo;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest req) {
        Long tenant = TenantContext.getCurrentTenant();
        Optional<User> u = userRepository.findByEmailAndSchoolId(req.getEmail().toLowerCase(), tenant);
        if (u.isEmpty()) return ResponseEntity.status(401).body("Invalid credentials");
        User user = u.get();
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) return ResponseEntity.status(401).body("Invalid credentials");

        String access = jwtUtil.generateAccessToken(user.getId(), user.getSchoolId());
        String refresh = UUID.randomUUID().toString();
        RefreshToken r = new RefreshToken();
        r.setUserId(user.getId());
        r.setTokenHash(passwordEncoder.encode(refresh));
        r.setExpiresAt(OffsetDateTime.now().plusDays(14));
        r.setCreatedAt(OffsetDateTime.now());
        refreshRepo.save(r);

        return ResponseEntity.ok(new AuthResponse(access, refresh));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ResetPasswordRequest req) {
        if (req.getSchoolId() == null || req.getEmail() == null || req.getCurrentPassword() == null
                || req.getNewPassword() == null || req.getConfirmPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "School ID, email and current/new password are required."));
        }

        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password confirmation does not match."));
        }

        String email = req.getEmail().trim().toLowerCase();
        Long tenant = req.getSchoolId();

        TenantContext.setCurrentTenant(tenant);
        try {
            Optional<User> optionalUser = userRepository.findByEmailAndSchoolId(email, tenant);
            if (optionalUser.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("message", "No user found for that school and email."));
            }

            User user = optionalUser.get();
            if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
                return ResponseEntity.status(401).body(Map.of("message", "Current password is incorrect."));
            }
            user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
            userRepository.save(user);
            refreshRepo.deleteByUserId(user.getId());

            return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
        } finally {
            TenantContext.clear();
        }
    }
}

class AuthRequest {
    private String email;
    private String password;
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

class ResetPasswordRequest {
    private Long schoolId;
    private String email;
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;

    public Long getSchoolId() { return schoolId; }
    public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}

class AuthResponse {
    private String accessToken;
    private String refreshToken;
    public AuthResponse(String a, String r) { this.accessToken = a; this.refreshToken = r; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
}
