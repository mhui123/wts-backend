package com.wts.kiwoom.service;

import com.wts.kiwoom.entity.KiwoomToken;
import com.wts.kiwoom.repository.KiwoomTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KiwoomTokenManager {

    private final KiwoomTokenRepository tokenRepository;

    @Value("${kiwoom.token.secret-key:change-me-to-32-char-secret-key!!}")
    private String encryptionKey;

    @Value("${kiwoom.token.ttl-minutes:1440}")
    private int tokenTtlMinutes;

    private static final String ALGORITHM = "AES/GCM/NoPadding"; // GCM 모드로 보안 강화 (IV와 인증 태그 필요)
    private static final int IV_LENGTH = 12; // GCM 권장 12 bytes
    private static final int TAG_LENGTH = 128; // 인증 태그 128bit

    /**
     * 키움 토큰을 데이터베이스에 안전하게 저장
     */
    @Transactional
    public String storeKiwoomToken(Long userId, String kiwoomToken) {
        try {
            // 기존 사용자의 활성 토큰들 비활성화 (한 번에 하나만 유지)
            tokenRepository.deactivateUserTokens(userId);

            // 새 토큰 ID 생성
            String tokenId = UUID.randomUUID().toString();

            // 키움 토큰 암호화
            String encryptedToken = encryptToken(kiwoomToken);

            // 만료 시간 설정
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(tokenTtlMinutes);

            // 데이터베이스에 저장
            KiwoomToken token = KiwoomToken.builder()
                    .userId(userId)
                    .tokenId(tokenId)
                    .encryptedToken(encryptedToken)
                    .expiresAt(expiresAt)
                    .build();

            tokenRepository.save(token);

            log.info("키움 토큰 저장 완료: userId={}, tokenId={}", userId, tokenId);
            return tokenId;

        } catch (Exception e) {
            log.error("키움 토큰 저장 실패: userId={}", userId, e);
            throw new RuntimeException("키움 토큰 저장 실패", e);
        }
    }

    /**
     * 토큰 ID로 실제 키움 토큰 조회
     */
    @Transactional(readOnly = true)
    public Optional<String> getKiwoomToken(Long userId, String tokenId) {
        try {
            Optional<KiwoomToken> tokenOpt = tokenRepository.findByUserIdAndTokenIdAndIsActiveTrue(userId, tokenId);

            if (tokenOpt.isEmpty()) {
                log.warn("키움 토큰을 찾을 수 없음: userId={}, tokenId={}", userId, tokenId);
                return Optional.empty();
            }

            KiwoomToken token = tokenOpt.get();

            // 만료 확인
            if (token.isExpired()) {
                log.warn("키움 토큰이 만료됨: userId={}, tokenId={}", userId, tokenId);
                return Optional.empty();
            }

            // 복호화 후 반환
            String decryptedToken = decryptToken(token.getEncryptedToken());
            return Optional.of(decryptedToken);

        } catch (Exception e) {
            log.error("키움 토큰 조회 실패: userId={}, tokenId={}", userId, tokenId, e);
            return Optional.empty();
        }
    }

    /**
     * 토큰 무효화
     */
    @Transactional
    public void invalidateToken(Long userId, String tokenId) {
        Optional<KiwoomToken> tokenOpt = tokenRepository.findByUserIdAndTokenIdAndIsActiveTrue(userId, tokenId);
        if (tokenOpt.isPresent()) {
            KiwoomToken token = tokenOpt.get();
            token.deactivate();
            tokenRepository.save(token);
            log.info("키움 토큰 무효화 완료: userId={}, tokenId={}", userId, tokenId);
        }
    }

    /**
     * 만료된 토큰들 정리 (스케줄러에서 호출)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            tokenRepository.deleteExpiredAndInactiveTokens(LocalDateTime.now());
            log.info("만료된 키움 토큰 정리 완료");
        } catch (Exception e) {
            log.error("만료된 키움 토큰 정리 실패", e);
        }
    }

    /**
     * 키움 토큰 암호화 (SHA-256 기반 키 생성)
     */
    private String encryptToken(String token) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);

        // SHA-256으로 항상 32바이트 키 생성
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha256.digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);

        // random IV 생성
        byte[] iv = new byte[IV_LENGTH];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);

        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);

        // IV 암호문 결합
        byte[] encrypted = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[IV_LENGTH + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
        System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * 키움 토큰 복호화 (SHA-256 기반 키 생성)
     */
    private String decryptToken(String encryptedToken) throws Exception {

        Cipher cipher = Cipher.getInstance(ALGORITHM);

        // 1️⃣ 키 생성 (암호화와 동일)
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha256.digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        byte[] decoded = Base64.getDecoder().decode(encryptedToken);

        // 2️⃣ IV 분리
        byte[] iv = Arrays.copyOfRange(decoded, 0, IV_LENGTH);
        byte[] encrypted = Arrays.copyOfRange(decoded, IV_LENGTH, decoded.length);

        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

        byte[] decrypted = cipher.doFinal(encrypted);

        return new String(decrypted, StandardCharsets.UTF_8);
    }
}