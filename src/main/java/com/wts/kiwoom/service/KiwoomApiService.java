package com.wts.kiwoom.service;

import com.wts.api.service.PythonServerService;
import com.wts.auth.JwtUtil;
import com.wts.kiwoom.dto.KeyDto;
import com.wts.kiwoom.dto.KiwoomApiRequest;
import com.wts.kiwoom.entity.KiwoomApiKey;
import com.wts.kiwoom.repository.KiwoomApiKeyRepository;
import com.wts.model.ProcessResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KiwoomApiService {
    private final PythonServerService pythonService;
    private final JwtUtil jwtUtil;
    private final KiwoomTokenManager kiwoomTokenManager;

    public ProcessResult getAccountInfo(KiwoomApiRequest req) {
        try {

            return null;
        } catch (Exception e) {
            log.error("키움 로그인 실패: ", e);
            return ProcessResult.builder()
                    .success(false)
                    .message("키움 로그인 실패: " + e.getMessage())
                    .build();
        }
    }

    public Optional<String> getKiwoomToken(String jwt) {
        // JWT에서 사용자 ID와 토큰 참조 추출
        String userId = jwtUtil.getSubject(jwt);
        String tokenRef = jwtUtil.getKiwoomTokenRef(jwt);

        return kiwoomTokenManager.getKiwoomToken(Long.valueOf(userId), tokenRef);
    }

}
