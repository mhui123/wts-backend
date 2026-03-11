package com.wts.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wts.auth.JwtUtil;
import com.wts.auth.jpa.entity.User;
import com.wts.auth.jpa.entity.UserPermission;
import com.wts.auth.jpa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRepository userRepository;
    private final UserPermissionRepository permissionRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public void register(String username, String password, String name) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password is required");
        }
        userRepository.findByEmail(username).ifPresent(u -> {
            throw new IllegalArgumentException("email already exists");
        });

        User user = new User();
        user.setProvider("local");
        user.setProviderId(username);
        user.setEmail(username);
        user.setName(name);
        user.setPictureUrl(null);
        user.setRoles("ROLE_USER");
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(password));

        userRepository.save(user);
    }

    public String login(String userName, String password) {
        if (userName == null || userName.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password is required");
        }

        User user = userRepository.findByEmail(userName)
                .orElseThrow(() -> new IllegalArgumentException("invalid credentials"));

        if (!user.isEnabled()) {
            throw new IllegalStateException("user is disabled");
        }

        String storedPassword = user.getPassword();
        if (storedPassword == null || !passwordEncoder.matches(password, storedPassword)) {
            throw new IllegalArgumentException("invalid credentials");
        }

        user.setLastLoginAt(java.time.LocalDateTime.now());
        User saved = userRepository.save(user);

        return jwtUtil.createToken(String.valueOf(saved.getId()));
    }

    public ResponseEntity<String> getMyInfo(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof User u) {
                Map<String, Object> dto = Map.of(
                        "id", u.getId(),
                        "email", u.getEmail(),
                        "name", u.getName(),
                        "pictureUrl", u.getPictureUrl() == null ? "" : u.getPictureUrl()
                );
                String json = objectMapper.writeValueAsString(dto);
                return ResponseEntity.ok(json);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Invalid principal");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    public boolean hasPermission(Authentication authentication, String requiredPermission) {
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            Long userId = user.getId();
            UserPermission permission = permissionRepository.findByUserId(userId);

            if (permission == null) {
                return false; // 권한 정보가 없음
            }

            return checkPermissionLevel(permission.getPermissionLevel(), requiredPermission);
        }
        return false; // 인증 정보가 없거나 principal이 User가 아님
    }

    private boolean checkPermissionLevel(UserPermission.UserPermissionLevel userLevel, String requiredLevel) {
        // 권한 계층 구조
        return switch (requiredLevel) {
            case "BASIC_USER" -> userLevel != null; // 모든 등록된 사용자
            case "TRADING_USER" -> userLevel == UserPermission.UserPermissionLevel.TRADING_USER ||
                    userLevel == UserPermission.UserPermissionLevel.ADMIN_USER;
            case "ADMIN_USER" -> userLevel == UserPermission.UserPermissionLevel.ADMIN_USER;
            default -> false;
        };
    }

}
