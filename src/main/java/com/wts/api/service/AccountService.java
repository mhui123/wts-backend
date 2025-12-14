package com.wts.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import com.wts.api.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class AccountService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Mono<ResponseEntity<String>> getMyInfo(Authentication authentication){
        return Mono.fromSupplier(() -> {
            try {
                if (authentication == null || !authentication.isAuthenticated()) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
                }
                Object principal = authentication.getPrincipal();
                if (principal instanceof User) {
                    User u = (User) principal;
                    Map<String, Object> dto = Map.of(
                            "id", u.getId(),
                            "email", u.getEmail(),
                            "name", u.getName(),
                            "pictureUrl", u.getPictureUrl()
                    );
                    String json = objectMapper.writeValueAsString(dto);
                    return ResponseEntity.ok(json);
                }
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Invalid principal");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
            }
        });
    }

}
