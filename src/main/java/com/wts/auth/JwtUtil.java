package com.wts.auth;

import com.wts.api.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JwtUtil {

    // 우선순위: 환경변수 APP_JWT_SECRET -> application property app.jwt.secret -> 기본값
    @Value("${APP_JWT_SECRET:${app.jwt.secret:change-me-please-change-this-to-a-secure-random-value}}")
    private String secret;

    @Value("${app.jwt.exp-ms:3600000}")
    private long expMs;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 매우 단순한 HS256 JWT 생성기.
     * 프로덕션에서는 키 길이/관리, 알고리즘 선택, 클레임 검증 등을 강화하세요.
     */
    public String createToken(String subject) {
        return createToken(subject, expMs);
    }

    /**
     * 만료시간을 지정할 수 있는 JWT 토큰 생성 메서드
     */
    public String createToken(String subject, long expirationMs) {
        try {
            long now = Instant.now().getEpochSecond();
            long exp = now + (expirationMs / 1000L);

            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", subject);
            payload.put("iat", now);
            payload.put("exp", exp);

            String headerJson = objectMapper.writeValueAsString(header);
            String payloadJson = objectMapper.writeValueAsString(payload);

            String encodedHeader = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

            String signingInput = encodedHeader + "." + encodedPayload;
            String signature = signHmacSha256(signingInput, secret);

            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create JWT", e);
        }
    }

    // 토큰 유효성 검사(서명 + 만료)
    public boolean validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;
            String signingInput = parts[0] + "." + parts[1];
            String expectedSig = signHmacSha256(signingInput, secret);
            if (!expectedSig.equals(parts[2])) return false;

            // exp 확인
            String payloadJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
            Map<?,?> payload = objectMapper.readValue(payloadJson, Map.class);
            Object expObj = payload.get("exp");
            if (expObj == null) return false;
            long exp = Long.parseLong(String.valueOf(expObj));
            long now = Instant.now().getEpochSecond();
            return now < exp;
        } catch (Exception e) {
            return false;
        }
    }

    // subject 추출
    public String getSubject(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            String payloadJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
            Map<?,?> payload = objectMapper.readValue(payloadJson, Map.class);
            Object subObj = payload.get("sub");
            return subObj != null ? String.valueOf(subObj) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static byte[] base64UrlDecode(String data) {
        return Base64.getUrlDecoder().decode(data);
    }

    private static String signHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(sig);
    }

    /**
     * 키움 토큰 참조 ID 포함 JWT 생성
     * @param subject 사용자 ID (문자열)
     * @param kiwoomTokenId 키움 토큰 참조 ID (선택사항)
     */
    public String createTokenWithKiwoomRef(long subject, String kiwoomTokenId) {
        try {
            long now = Instant.now().getEpochSecond();
            long exp = now + (expMs / 1000L);

            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", subject);  // 사용자 ID
            payload.put("iat", now);
            payload.put("exp", exp);

            // 키움 토큰 참조 ID만 저장 (안전함)
            if (kiwoomTokenId != null && !kiwoomTokenId.trim().isEmpty()) {
                payload.put("kiwoom_ref", kiwoomTokenId);
            }

            String headerJson = objectMapper.writeValueAsString(header);
            String payloadJson = objectMapper.writeValueAsString(payload);

            String encodedHeader = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

            String signingInput = encodedHeader + "." + encodedPayload;
            String signature = signHmacSha256(signingInput, secret);

            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create JWT", e);
        }
    }

    /**
     * 키움 토큰 참조 ID 추출
     */
    public String getKiwoomTokenRef(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            String payloadJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
            Map<?,?> payload = objectMapper.readValue(payloadJson, Map.class);
            Object refObj = payload.get("kiwoom_ref");
            return refObj != null ? String.valueOf(refObj) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Authentication 객체에서 사용자 ID를 추출하는 헬퍼 메서드
     */
    public Long extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }
}
