package com.wts.kiwoom.service;

import com.wts.api.service.PythonServerService;
import com.wts.auth.JwtUtil;
import com.wts.kiwoom.dto.KeyDto;
import com.wts.kiwoom.entity.KiwoomApiKey;
import com.wts.model.ProcessResult;
import com.wts.util.MapCaster;
import com.wts.util.UtilsForRequest;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KiwoomPublicService {
    private final KiwoomKeyService keyService;
    private final PythonServerService pythonService;
    private final JwtUtil jwtUtil;
    private final MapCaster caster;
    private final KiwoomTokenManager kiwoomTokenManager;
    private final KiwoomApiService apiService;
    private final UtilsForRequest uRe;

    public ProcessResult saveKiwoomKey(Long userId, String appKey, String appSecret) {
        try {

            if(validateApiKey(appKey, appSecret, userId)){
                boolean result = writeKiwoomKey(userId, appKey, appSecret);

                if(result){
                    return ProcessResult.builder()
                            .success(true)
                            .message("Kiwoom API Key 저장 성공")
                            .build();
                } else {
                    return ProcessResult.builder()
                            .success(false)
                            .message("Kiwoom API Key 저장 실패")
                            .build();
                }
            } else {
                log.error("키움 키 저장 실패 : ", new ValidationException("Invalid Kiwoom API Key"));
                return ProcessResult.builder()
                        .success(false)
                        .message("키움 키 저장 실패 - key 인증에 실패하였습니다.")
                        .build();
            }

        }
        catch (DataIntegrityViolationException e) {
            log.error("키움 키 저장 실패 : ", e);
            return ProcessResult.builder()
                    .success(false)
                    .message("키움 키 저장 실패 - 무효한 데이터 입력.")
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


    public boolean writeKiwoomKey(Long userId, String appKey, String appSecret) {
        try {
            log.info("키움 키 저장 처리 시작: userId={}", userId);

            KiwoomApiKey wroteKey = keyService.registerNewKey(userId, appKey, appSecret);
            if(wroteKey.getIsActive()){
                log.info("키움 키 저장 성공: userId={}", userId);
                return true;
            } else {
                log.error("키움 키 저장 실패 - 활성화 실패: userId={}", userId);
                return false;
            }
        }
        catch (DataIntegrityViolationException e) {
            log.error("키움 키 저장 실패 : ", e);
            return false;
        }
        catch (Exception e) {
            log.error("키움 키 저장 실패: ", e);
            return false;
        }
    }

    public ProcessResult kiwoomLogin(Long userId) {
        try {
            Optional<KiwoomApiKey> apiKeyOpt = keyService.getActiveKey(userId);
            KiwoomApiKey apiKey;
            if(apiKeyOpt.isPresent()){
                apiKey = apiKeyOpt.get();
            } else {
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

            String token = uRe.getKiwoomTokenFromJwt(jwt);

            log.info("키움 로그아웃 처리 시작: userId={}", userId);
            Optional<KiwoomApiKey> apiKeyOpt = keyService.getActiveKey(userId);
            KiwoomApiKey apiKey;
            if(apiKeyOpt.isPresent()){
                apiKey = apiKeyOpt.get();
            } else {
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

    public ProcessResult checkKiwoomKey(long userId) {
        try {
            Optional<KiwoomApiKey> apiKeyOpt = keyService.getActiveKey(userId);
            if (apiKeyOpt.isPresent()) {
                KiwoomApiKey info = apiKeyOpt.get();
                if(info.getIsActive()){
                    return ProcessResult.success("등록된 키움 API 키가 있습니다.");
                } else {
                    return ProcessResult.failure("등록된 키움 API 키가 비활성화 상태입니다.");
                }

            } else {
                return ProcessResult.failure("등록된 키움 API 키가 없습니다.");
            }
        } catch (Exception e) {
            log.error("키움 키 확인 실패: ", e);
            return ProcessResult.failure("키움 키 확인 실패: " + e.getMessage());
        }
    }

    public boolean validateApiKey(String appKey, String appSecret, long userId) {
        try {
            KeyDto keyDto = KeyDto.builder()
                    .appKey(appKey)
                    .appSecret(appSecret)
                    .userId(userId)
                    .build();
            ProcessResult result = pythonService.kiwoomLogin(keyDto);
            //키움api의 접근권한획득에 성공하면 키가 유효하다고 판단한다.

            return result.isSuccess();
        } catch (Exception e) {
            log.error("키움 API 키 유효성 검사 실패: ", e);
            return false;
        }
    }
}
