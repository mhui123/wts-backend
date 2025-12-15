package com.wts.kiwoom;

import com.wts.api.service.PythonServerService;
import com.wts.auth.dto.JwtResponse;
import com.wts.kiwoom.dto.KiwoomApiRequest;
import com.wts.kiwoom.dto.KiwoomTokenRequest;
import com.wts.kiwoom.service.KiwoomApiService;
import com.wts.kiwoom.service.KiwoomAuthService;
import com.wts.model.ProcessResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Kiwoom 인증 컨트롤러 (프루빙 방식)
 * - 클라이언트가 전달한 키움 접근토큰으로 무해한 API를 호출하여 유효성을 판단합니다.
 * - 유효하면 내부 JWT를 만들어 반환합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kiwoom")
@Slf4j
public class KiwoomAuthController {

    private final KiwoomAuthService authService;
    private final PythonServerService pythonServerService;
    private final KiwoomApiService kiwoomApiService;

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticateWithKiwoom(@RequestBody KiwoomTokenRequest req) {
        if (req == null || req.getKiwoomToken() == null || req.getKiwoomToken().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("missing token");
        }
        log.info("/auth/kiwoom probe-validate called");
        String jwt = authService.verifyKiwoomTokenAndCreateJwt(req.getKiwoomToken());
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Kiwoom token invalid");
        }
        return ResponseEntity.ok(new JwtResponse(jwt));
    }

    @PostMapping("/login")
    public ResponseEntity<ProcessResult> kiwoomLogin(@RequestBody KiwoomApiRequest req) {
        Long userId = req.getUserId();
        log.info("키움 로그인 요청: userId={}", userId);
        try {
            ProcessResult response = kiwoomApiService.kiwoomLogin(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("키움 로그인 실패: ", e);
            ProcessResult errorResponse = ProcessResult.builder()
                    .success(false)
                    .message("키움 로그인 실패: " + e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/writeKey")
    public ResponseEntity<ProcessResult> writeKey(@RequestBody KiwoomApiRequest req) {
        Long userId = req.getUserId();
        String appKey = req.getAppKey();
        String appSecret = req.getAppSecret();

        try {
            ProcessResult response = kiwoomApiService.writeKiwoomKey(userId, appKey, appSecret);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("키움 키저장 실패: ", e);
            ProcessResult errorResponse = ProcessResult.builder()
                    .success(false)
                    .message("키움 키저장 실패: " + e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ProcessResult> kiwoomLogout(@RequestBody KiwoomApiRequest req) {
        Long userId = req.getUserId();
        try {
            ProcessResult response = kiwoomApiService.kiwoomLogout(req);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("키움 로그인 실패: ", e);
            ProcessResult errorResponse = ProcessResult.builder()
                    .success(false)
                    .message("키움 로그인 실패: " + e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
