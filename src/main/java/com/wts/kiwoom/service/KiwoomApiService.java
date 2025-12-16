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
    private final PythonServerService pythonService;

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

}
