// 계좌 관련 REST 컨트롤러: 외부 Kiwoom 어댑터를 통해 계좌 잔액 정보를 조회하고 반환합니다.
// 주요 책임: /api/account 경로의 엔드포인트 제공, 비동기 반환(Mono)
package com.wts.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wts.api.service.AccountService;
import com.wts.entity.User;
import com.wts.infra.KiwoomAdapterClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final KiwoomAdapterClient adapter;
    @Autowired
    private AccountService accountService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AccountController(KiwoomAdapterClient adapter) {
        this.adapter = adapter;
    }

    @GetMapping("/balance")
    public Mono<ResponseEntity<String>> balance() {
        return adapter.getBalance().map(ResponseEntity::ok);
    }

    @GetMapping("/getMyInfo")
    public Mono<ResponseEntity<String>> getMyInfo(Authentication authentication) {
        return accountService.getMyInfo(authentication);
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<String>> logout(HttpServletRequest request, HttpServletResponse response) {
        return Mono.fromSupplier(() -> {
            try {
                // 1) 세션 무효화
                var session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                // 2) 쿠키 삭제: JWT, JSESSIONID (SameSite=None, Secure, HttpOnly 유지)
                response.addHeader("Set-Cookie", "JWT=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None");
                response.addHeader("Set-Cookie", "JSESSIONID=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None");
                return ResponseEntity.ok("logged_out");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
            }
        });
    }

}
