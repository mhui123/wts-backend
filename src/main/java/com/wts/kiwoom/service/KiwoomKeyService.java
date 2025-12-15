package com.wts.kiwoom.service;

import com.wts.kiwoom.dto.KeyDto;
import com.wts.kiwoom.entity.KiwoomApiKey;
import com.wts.kiwoom.repository.KiwoomApiKeyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

@Service
public class KiwoomKeyService {

    private TextEncryptor encryptor;

    public KiwoomKeyService(@Value("${kiwoom.encryption.secret}") String encryptionSecret,
                            @Value("${kiwoom.encryption.salt}") String salt) {
        // 환경변수 검증 - 기본값 체크 추가
        if (encryptionSecret == null || encryptionSecret.trim().isEmpty() ||
                encryptionSecret.equals("PLEASE-SET-ENVIRONMENT-VARIABLE")) {
            throw new IllegalStateException("KIWOOM_ENCRYPTION_SECRET 환경변수가 설정되지 않았습니다. " +
                    "다음 중 하나를 선택하세요:\n" +
                    "1. 환경변수 설정: set KIWOOM_ENCRYPTION_SECRET=your-32-character-key\n" +
                    "2. application.yml에 실제 값 설정");
        }

        // 암호화 키 길이 검증 (최소 32자 권장)
        if (encryptionSecret.length() < 32) {
            throw new IllegalStateException("암호화 키는 최소 32자 이상이어야 합니다. 현재 길이: " + encryptionSecret.length());
        }

        // Salt 검증 - 기본값 체크 추가
        if (salt == null || salt.equals("PLEASE-SET-ENVIRONMENT-VARIABLE")) {
            throw new IllegalStateException("KIWOOM_ENCRYPTION_SALT 환경변수가 설정되지 않았습니다. " +
                    "다음 중 하나를 선택하세요:\n" +
                    "1. 환경변수 설정: set KIWOOM_ENCRYPTION_SALT=32자리16진수\n" +
                    "2. application.yml에 실제 값 설정");
        }

        // Salt 길이 검증 (정확히 32자 = 16바이트 Hex)
        if (salt.length() != 32) {
            throw new IllegalStateException("Salt는 정확히 32자(16바이트 Hex)여야 합니다. 현재 길이: " + salt.length());
        }

        // Hex 형태 검증
        if (!salt.matches("^[0-9a-fA-F]{32}$")) {
            throw new IllegalStateException("Salt는 32자의 Hex 문자열이어야 합니다.");
        }

        try {
            this.encryptor = Encryptors.text(encryptionSecret, salt);
        } catch (Exception e) {
            throw new IllegalStateException("암호화 모듈 초기화 실패: " + e.getMessage(), e);
        }
    }

    public String encrypt(String plainText) {
        if (encryptor == null) {
            throw new IllegalStateException("Encryptor가 초기화되지 않았습니다.");
        }
        return encryptor.encrypt(plainText);
    }

    public String decrypt(String encryptedText) {
        if (encryptor == null) {
            throw new IllegalStateException("Encryptor가 초기화되지 않았습니다.");
        }
        return encryptor.decrypt(encryptedText);
    }

    public KeyDto makeKeyDto(KiwoomApiKey apiKey) {
        if (apiKey == null) {
            throw new IllegalArgumentException("KiwoomApiKey가 null입니다.");
        }

        String decryptedAppKey = decrypt(apiKey.getEncryptedAppKey());
        String decryptedAppSecret = decrypt(apiKey.getEncryptedSecretKey());

        return KeyDto.builder()
                .appKey(decryptedAppKey)
                .appSecret(decryptedAppSecret)
                .userId(apiKey.getUserId())
                .build();
    }
}
