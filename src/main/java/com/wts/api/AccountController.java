package com.wts.api;

import com.wts.api.service.AccountService;
import com.wts.infra.KiwoomAdapterClient;
import com.wts.kiwoom.dto.KiwoomApiRequest;
import com.wts.kiwoom.service.KiwoomPublicService;
import com.wts.model.ProcessResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final KiwoomAdapterClient adapter;
    @Autowired
    private AccountService accountService;
    @Autowired
    private KiwoomPublicService kiwoomPublicService;

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
    public Mono<ResponseEntity<String>> logout(HttpServletRequest request, HttpServletResponse response, @RequestBody KiwoomApiRequest req) {
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

                //키움 토큰 폐기추가
                ProcessResult responseDto = kiwoomPublicService.kiwoomLogout(req);
                return ResponseEntity.ok("logged_out");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
            }
        });
    }

}
