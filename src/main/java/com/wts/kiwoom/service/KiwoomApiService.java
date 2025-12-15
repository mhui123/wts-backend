package com.wts.kiwoom.service;

import com.wts.api.service.PythonServerService;
import com.wts.kiwoom.dto.KeyDto;
import com.wts.kiwoom.dto.KiwoomApiRequest;
import com.wts.kiwoom.entity.KiwoomApiKey;
import com.wts.kiwoom.repository.KiwoomApiKeyRepository;
import com.wts.model.ProcessResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KiwoomApiService {
    private final KiwoomKeyService keyService;
    private final KiwoomApiKeyRepository keyRepo;
    private final PythonServerService pythonService;

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
            return pythonService.kiwoomLogin(keyDto);
        } catch (Exception e) {
            log.error("키움 로그인 실패: ", e);
            return ProcessResult.builder()
                    .success(false)
                    .message("키움 로그인 실패: " + e.getMessage())
                    .build();
        }
    }

    public ProcessResult kiwoomLogout(KiwoomApiRequest req) {
        try {
            Long userId = req.getUserId();
            log.info("키움 로그아웃 처리 시작: userId={}", userId);

            KiwoomApiKey apiKey = keyRepo.findByUserId(userId);
            if (apiKey == null) {
                return ProcessResult.builder()
                        .success(false)
                        .message("등록된 키움 API 키가 없습니다.")
                        .build();
            }

            KeyDto keyDto = keyService.makeKeyDto(apiKey);
            keyDto.setToken(req.getToken());

            ProcessResult result =pythonService.kiwoomLogout(keyDto);
            log.info("로그아웃 성공: {}", userId);
            return result;
        } catch (Exception e) {
            log.error("키움 로그인 실패: ", e);
            return ProcessResult.builder()
                    .success(false)
                    .message("키움 로그인 실패: " + e.getMessage())
                    .build();
        }
    }
}
