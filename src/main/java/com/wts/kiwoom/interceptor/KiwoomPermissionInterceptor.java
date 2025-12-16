package com.wts.kiwoom.interceptor;

import com.wts.auth.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class KiwoomPermissionInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    public KiwoomPermissionInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.info("KiwoomPermissionInterceptor: preHandle called for URI: {}", request.getRequestURI());
        // 1단계: 기본 인증 검증 (JWT 토큰이 유효한가?)
       String token = extractTokenFromHeader(request);
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }

//        // 2단계: 키움 API 키가 등록되어 있는가?
//        String userId = jwtService.getUserIdFromToken(token);
//        if (!kiwoomKeyService.hasValidApiKey(userId)) {
//            response.setStatus(HttpStatus.FORBIDDEN.value());
//            return false;
//        }
//
//        // 3단계: 요청 제한 확인 (일일 호출 제한)
//        if (!rateLimitService.isWithinLimit(userId)) {
//            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
//            return false;
//        }

        return true; // 기본 검증 통과
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}