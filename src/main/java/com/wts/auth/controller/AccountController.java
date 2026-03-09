package com.wts.auth.controller;

import com.wts.api.dto.ProcessResult;
import com.wts.auth.dto.RegisterRequest;
import com.wts.auth.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {
    @Autowired
    private AccountService accountService;

    @GetMapping("/getMyInfo")
    public ResponseEntity<String> getMyInfo(Authentication authentication) {
        return accountService.getMyInfo(authentication);
    }

    @PostMapping("/register")
    public ResponseEntity<ProcessResult> register(@RequestBody RegisterRequest request) {
        try {

            accountService.register(request.getEmail(), request.getPassword(), request.getName());
            return ResponseEntity.ok(ProcessResult.success("회원가입 성공"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ProcessResult.failure("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ProcessResult> login(@RequestBody RegisterRequest request,
                                               HttpServletResponse response) {
        try {
            String jwt = accountService.login(request.getEmail(), request.getPassword());

            int maxAge = 60 * 60 * 24; // 1 day
            String cookieValue = URLEncoder.encode(jwt, StandardCharsets.UTF_8);
            String setCookie = "JWT=" + cookieValue
                    + "; Path=/"
                    + "; Max-Age=" + maxAge
                    + "; HttpOnly"
                    + "; Secure"
                    + "; SameSite=None";
            response.addHeader("Set-Cookie", setCookie);
            ProcessResult result = ProcessResult.builder()
                    .data(jwt).build();

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ProcessResult.failure("Error: " + e.getMessage()));
        }
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
