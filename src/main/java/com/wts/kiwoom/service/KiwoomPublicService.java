package com.wts.kiwoom.service;

import com.wts.api.service.PythonServerService;
import com.wts.auth.JwtUtil;
import com.wts.kiwoom.dto.KeyDto;
import com.wts.kiwoom.dto.KiwoomApiRequest;
import com.wts.kiwoom.entity.KiwoomApiKey;
import com.wts.kiwoom.entity.KiwoomToken;
import com.wts.kiwoom.repository.KiwoomApiKeyRepository;
import com.wts.model.ProcessResult;
import com.wts.util.MapCaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KiwoomPublicService {
    private final KiwoomKeyService keyService;
    private final KiwoomApiKeyRepository keyRepo;
    private final PythonServerService pythonService;
    private final JwtUtil jwtUtil;
    private final MapCaster caster;
    private final KiwoomTokenManager kiwoomTokenManager;
    private final KiwoomApiService apiService;

    public ProcessResult writeKiwoomKey(Long userId, String appKey, String appSecret) {
        try {
            log.info("키움 키 저장 처리 시작: userId={}", userId);

            String encryptedAppKey = keyService.encrypt(appKey);
            String encryptedAppSecret = keyService.encrypt(appSecret);

            KiwoomApiKey apiKey = KiwoomApiKey.builder()
                    .userId(userId)
                    .encryptedAppKey(encryptedAppKey)
                    .encryptedSecretKey(encryptedAppSecret)
                    .build();

            keyRepo.save(apiKey);

            return ProcessResult.builder()
                    .success(true)
                    .message("Kiwoom API Key 저장 성공")
                    .build();
        }
        catch (DataIntegrityViolationException e) {
            log.error("키움 키 저장 실패 - 중복된 사용자 ID: ", e);
            return ProcessResult.builder()
                    .success(false)
                    .message("키움 키 저장 실패 - 중복된 사용자 ID입니다.")
                    .build();
        }
        catch (Exception e) {
            log.error("키움 키 저장 실패: ", e);
            return ProcessResult.builder()
                    .success(false)
                    .message("키움 키 저장 실패: " + e.getMessage())
                    .build();
        }
    }

    public ProcessResult kiwoomLogin(Long userId) {
        try {
            log.info("키움 로그인 처리 시작: userId={}", userId);

            KiwoomApiKey apiKey = keyRepo.findByUserId(userId);
            if (apiKey == null) {
                return ProcessResult.builder()
                        .success(false)
                        .message("등록된 키움 API 키가 없습니다.")
                        .build();
            }
            KeyDto keyDto = keyService.makeKeyDto(apiKey);
            ProcessResult result = pythonService.kiwoomLogin(keyDto);

            Map<String, Object> data = caster.safeMapCast(result.getData());
            String kiwoomToken = caster.safeMapGetString(data, "token");

            // 키움 토큰을 서버에 안전하게 저장
            String tokenId = kiwoomTokenManager.storeKiwoomToken(userId, kiwoomToken);
            // JWT에는 토큰 참조 ID만 포함
            String jwt = jwtUtil.createTokenWithKiwoomRef(userId, tokenId);

            Map<String, String> tokenMap = Map.of("jwt", jwt);
            result.setData(tokenMap);

            return result;

        } catch (Exception e) {
            log.error("키움 로그인 실패: ", e);
            return ProcessResult.builder()
                    .success(false)
                    .message("키움 로그인 실패: " + e.getMessage())
                    .build();
        }
    }

    public ProcessResult kiwoomLogout(String jwt) {
        try {
            long userId = Long.parseLong(jwtUtil.getSubject(jwt));
            String tokenId = jwtUtil.getKiwoomTokenRef(jwt);

            Optional<String> kiwoomToken = kiwoomTokenManager.getKiwoomToken(userId, tokenId);

            if (kiwoomToken.isEmpty()) {
                String msg = String.format("키움 토큰을 찾을 수 없음: userId=%d", userId);
                log.warn(msg);
                return ProcessResult.failure(msg);
            }

            String token = kiwoomToken.get();

            log.info("키움 로그아웃 처리 시작: userId={}", userId);

            KiwoomApiKey apiKey = keyRepo.findByUserId(userId);
            if (apiKey == null) {
                return ProcessResult.failure("등록된 키움 API 키가 없습니다.");
            }

            KeyDto keyDto = keyService.makeKeyDto(apiKey);
            keyDto.setToken(token);

            ProcessResult result = pythonService.kiwoomLogout(keyDto);
            kiwoomTokenManager.invalidateToken(userId, tokenId);

            log.info("로그아웃 성공: {}", userId);
            return result;
        } catch (Exception e) {
            return ProcessResult.failure(String.format("알 수 없는 에러: %s", e.getMessage()));
        }
    }
}
