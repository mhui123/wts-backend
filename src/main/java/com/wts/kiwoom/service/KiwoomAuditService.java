package com.wts.kiwoom.service;

import com.wts.kiwoom.entity.KiwoomAuditLog;
import com.wts.kiwoom.entity.KiwoomStatus;
import com.wts.kiwoom.repository.KiwoomAuditRepository;
import com.wts.api.dto.ProcessResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class KiwoomAuditService {

    private final KiwoomAuditRepository auditRepository;
    private final Logger logger = LoggerFactory.getLogger(KiwoomAuditService.class);

    public KiwoomAuditService(KiwoomAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Async
    public void logApiRequest(long userId, String apiEndpoint, long executionTime, ProcessResult result) {
        try {
            Boolean status = result.isSuccess();
            KiwoomStatus st = determineStatus(status);
            KiwoomAuditLog auditLog = KiwoomAuditLog.builder()
                    .userId(userId)
                    .apiEndpoint(apiEndpoint)
                    .timestamp(LocalDateTime.now())
                    .status(st)
                    .executionTime(executionTime)  // 실행시간 추가
                    .build();

            if(st != KiwoomStatus.SUCCESS){
                String msg = result.getMessage();
                auditLog.setErrorMessage(msg);
            }

            auditRepository.save(auditLog);

            // 파일 로깅
            logger.info("Kiwoom API Call - User: {}, Endpoint: {}, ExecutionTime: {}ms, Status: {}",
                    userId, apiEndpoint, executionTime, st);

        } catch (Exception e) {
            // 감사 로깅 실패가 메인 로직에 영향을 주지 않도록 예외 처리
            logger.error("Failed to log audit information for user: {}, endpoint: {}", userId, apiEndpoint, e);
        }
    }

    @Async
    public void logSecurityEvent(String userId, String eventType, String details) {
        // 보안 관련 이벤트 로깅 (키 조회, 권한 변경 등)
    }

    private KiwoomStatus determineStatus(Boolean status) {
        if (status == null) {
            return KiwoomStatus.TIMEOUT;
        }
        return status ? KiwoomStatus.SUCCESS : KiwoomStatus.ERROR;
    }
}