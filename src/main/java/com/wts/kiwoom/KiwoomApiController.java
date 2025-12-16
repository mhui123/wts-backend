package com.wts.kiwoom;

import com.wts.kiwoom.dto.KiwoomApiRequest;
import com.wts.kiwoom.service.KiwoomApiService;
import com.wts.kiwoom.service.KiwoomPublicService;
import com.wts.kiwoom.service.KiwoomAuditService;
import com.wts.model.ProcessResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kiwoom")
@RequiredArgsConstructor
public class KiwoomApiController {

    private final KiwoomAuditService auditService;
    private final KiwoomApiService apiService;
    // 조회 권한만 있으면 호출 가능
    @PostMapping("/account/balance")
    @PreAuthorize("@kiwoomPermissionService.hasPermission(authentication.name, 'BASIC_USER')")
    public ResponseEntity<?> getAccountBalance(@RequestBody KiwoomApiRequest req){
        long startTime = System.currentTimeMillis();

        try {
            // 계좌 잔고 조회 로직
            ProcessResult result = apiService.getAccountInfo(req);
            // 성공 결과 생성
//            ProcessResult result = ProcessResult.success("계좌 잔고 조회 완료");

            // 비동기 감사 로깅
            long executionTime = System.currentTimeMillis() - startTime;
            auditService.logApiRequest(
                    req.getUserId(),
                    "/api/kiwoom/account/balance",
                    executionTime,
                    result
            );

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            // 실패 결과 생성
            ProcessResult result = ProcessResult.failure("계좌 잔고 조회 실패: " + e.getMessage());

            // 비동기 감사 로깅 (에러 포함)
            long executionTime = System.currentTimeMillis() - startTime;
            auditService.logApiRequest(
                    req.getUserId(),
                    "/api/kiwoom/account/balance",
                    executionTime,
                    result
            );

            return ResponseEntity.internalServerError().body("처리 중 오류가 발생했습니다.");
        }
    }

//    // 거래 권한이 있어야 호출 가능
//    @PostMapping("/trade/order")
//    @PreAuthorize("@kiwoomPermissionService.hasPermission(authentication.name, 'TRADING_USER')")
//    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request) {
//        // 주문 실행
//    }
//
//    // 관리자만 호출 가능
//    @PostMapping("/admin/users")
//    @PreAuthorize("@kiwoomPermissionService.hasPermission(authentication.name, 'ADMIN_USER')")
//    public ResponseEntity<?> manageUsers(@RequestBody AdminRequest request) {
//        // 사용자 관리
//    }
}
