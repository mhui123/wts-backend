package com.wts.api.web;

import com.wts.api.service.AccountService;
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
    @Autowired
    private AccountService accountService;
    @Autowired
    private KiwoomPublicService kiwoomPublicService;

    @GetMapping("/getMyInfo")
    public Mono<ResponseEntity<String>> getMyInfo(Authentication authentication) {
        return accountService.getMyInfo(authentication);
    }

    @PostMapping("/logout")
    public ResponseEntity<ProcessResult> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 1) 세션 무효화
            var session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            // 2) 쿠키 삭제: JWT, JSESSIONID (SameSite=None, Secure, HttpOnly 유지)
            response.addHeader("Set-Cookie", "JWT=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None");
            response.addHeader("Set-Cookie", "JSESSIONID=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None");


            return ResponseEntity.ok(ProcessResult.success("로그아웃 성공"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ProcessResult.failure("Error: " + e.getMessage()));
        }
    }

}
