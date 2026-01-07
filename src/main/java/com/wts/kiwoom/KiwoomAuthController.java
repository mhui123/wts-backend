package com.wts.kiwoom;

import com.wts.api.entity.User;
import com.wts.api.service.PythonServerService;
import com.wts.auth.JwtUtil;
import com.wts.auth.dto.JwtResponse;
import com.wts.kiwoom.dto.KiwoomApiRequest;
import com.wts.kiwoom.dto.KiwoomTokenRequest;
import com.wts.kiwoom.service.KiwoomPublicService;
import com.wts.kiwoom.service.KiwoomTokenManager;
import com.wts.model.ProcessResult;
import com.wts.util.UtilsForRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Kiwoom 인증 컨트롤러 (프루빙 방식)
 * - 클라이언트가 전달한 키움 접근토큰으로 무해한 API를 호출하여 유효성을 판단합니다.
 * - 유효하면 내부 JWT를 만들어 반환합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kiwoom/public")
@Slf4j
public class KiwoomAuthController {

    private final PythonServerService pythonServerService;
    private final KiwoomPublicService kiwoomPublicService;
    private final UtilsForRequest uRe;
    private final JwtUtil jwtUtil;

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticateWithKiwoom(@RequestBody KiwoomTokenRequest req) {
        log.info("/authenticate");
        if (req == null || req.getKiwoomToken() == null || req.getKiwoomToken().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("missing token");
        }
        log.info("/auth/kiwoom probe-validate called");
        String jwt = pythonServerService.verifyKiwoomTokenAndCreateJwt(req.getKiwoomToken());
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Kiwoom token invalid");
        }

        return ResponseEntity.ok(new JwtResponse(jwt));
    }

    @PostMapping("/login")
    public ResponseEntity<ProcessResult> kiwoomLogin(@RequestBody KiwoomApiRequest req, Authentication authentication) {
        Long userId = jwtUtil.extractUserIdFromAuthentication(authentication);
        log.info("키움 로그인 요청: userId={}", userId);
        try {
            ProcessResult response = kiwoomPublicService.kiwoomLogin(userId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            String msg = String.format("키움 로그인 실패: %s", e);
            log.error(msg);
            ProcessResult errorResponse = ProcessResult.failure(msg);
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/writeKey")
    public ResponseEntity<ProcessResult> writeKey(@RequestBody KiwoomApiRequest req, Authentication authentication) {
        Long userId = jwtUtil.extractUserIdFromAuthentication(authentication);
        String appKey = req.getAppKey();
        String appSecret = req.getAppSecret();

        try {
            //바로 db에 저장하고있음. 키움api에서 먼저 검증 후 저장하도록 변경.
            ProcessResult response = kiwoomPublicService.saveKiwoomKey(userId, appKey, appSecret);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            String msg = String.format("키움 키저장 실패: %s", e);
            log.error(msg);
            ProcessResult errorResponse = ProcessResult.failure(msg);
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ProcessResult> kiwoomLogout(HttpServletRequest request) {
        String jwt;
        jwt = uRe.attractJwtFromRequest(request);
        if( jwt == null){
            String msg = "Authorization 헤더에서 JWT를 찾을 수 없습니다.";
            log.warn(msg);
            ProcessResult errorResponse = ProcessResult.failure(msg);
            return ResponseEntity.internalServerError().body(errorResponse);
        }

        try {
            ProcessResult response = kiwoomPublicService.kiwoomLogout(jwt);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            String msg = String.format("키움 로그아웃 실패: %s", e);
            log.error(msg);
            ProcessResult errorResponse = ProcessResult.failure(msg);
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/checkApiKey")
    public ResponseEntity<ProcessResult> checkKey(Authentication authentication) {
        try {
            Long userId = jwtUtil.extractUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().build();
            }
            ProcessResult response = kiwoomPublicService.checkKiwoomKey(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            String msg = String.format("키움 키 상태 확인 실패: %s", e);
            log.error(msg);
            ProcessResult errorResponse = ProcessResult.failure(msg);
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
