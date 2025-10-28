package com.wts.auth;

import org.springframework.beans.factory.annotation.Value;
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
        try {
            long now = Instant.now().getEpochSecond();
            long exp = now + (expMs / 1000L);

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

    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static String signHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(sig);
    }
}
