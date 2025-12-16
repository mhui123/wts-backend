package com.wts.api;

import com.wts.api.service.AccountService;
import com.wts.infra.KiwoomAdapterClient;
import com.wts.kiwoom.dto.KiwoomApiRequest;
import com.wts.kiwoom.service.KiwoomPublicService;
import com.wts.model.ProcessResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final KiwoomAdapterClient adapter;
    @Autowired
    private AccountService accountService;
    @Autowired
    private KiwoomPublicService kiwoomPublicService;

    @GetMapping("/balance")
    public Mono<ResponseEntity<String>> balance() {
        return adapter.getBalance().map(ResponseEntity::ok);
    }

    @GetMapping("/getMyInfo")
    public Mono<ResponseEntity<String>> getMyInfo(Authentication authentication) {
        return accountService.getMyInfo(authentication);
    }

    @PostMapping("/logout")
    public ResponseEntity<ProcessResult> logout(HttpServletRequest request, HttpServletResponse response, @RequestBody KiwoomApiRequest req) {
        try {
            // 1) 세션 무효화
            var session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            // 2) 쿠키 삭제: JWT, JSESSIONID (SameSite=None, Secure, HttpOnly 유지)
            response.addHeader("Set-Cookie", "JWT=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None");
            response.addHeader("Set-Cookie", "JSESSIONID=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None");

            // Authorization 헤더 추출 (대소문자 구분 없이)
            String jwt = null;
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null) {
                // 대소문자 변형 확인
                authHeader = request.getHeader("authorization");
            }

            if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
                jwt = authHeader.substring(7).trim();
            } else {
                String msg = String.format("유효하지 않은 Authorization 헤더: %s", authHeader);
                ProcessResult errorResponse = ProcessResult.failure(msg);

                return ResponseEntity.internalServerError().body(errorResponse);
            }
            //키움 토큰 폐기추가
            ProcessResult result = kiwoomPublicService.kiwoomLogout(jwt);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ProcessResult.failure("Error: " + e.getMessage()));
        }
    }

}
