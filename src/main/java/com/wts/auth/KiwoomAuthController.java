package com.wts.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;

/**
 * Kiwoom 인증 컨트롤러 (프루빙 방식)
 * - 클라이언트가 전달한 키움 접근토큰으로 무해한 API를 호출하여 유효성을 판단합니다.
 * - 유효하면 내부 JWT를 만들어 반환합니다.
 */
@RestController
@RequestMapping("/auth")
public class KiwoomAuthController {

    private static final Logger log = LoggerFactory.getLogger(KiwoomAuthController.class);

    private final KiwoomAuthService authService;

    @Autowired
    public KiwoomAuthController(KiwoomAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/kiwoom")
    public ResponseEntity<?> authenticateWithKiwoom(@RequestBody KiwoomTokenRequest req) {
        if (req == null || req.getKiwoomToken() == null || req.getKiwoomToken().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("missing token");
        }
        log.info("/auth/kiwoom probe-validate called");
        String jwt = authService.verifyKiwoomTokenAndCreateJwt(req.getKiwoomToken());
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Kiwoom token invalid");
        }
        return ResponseEntity.ok(new JwtResponse(jwt));
    }

    public static class KiwoomTokenRequest {
        @NotBlank
        private String kiwoomToken;

        public String getKiwoomToken() {
            return kiwoomToken;
        }

        public void setKiwoomToken(String kiwoomToken) {
            this.kiwoomToken = kiwoomToken;
        }
    }

    public static class JwtResponse {
        private final String token;

        public JwtResponse(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }
    }
}
