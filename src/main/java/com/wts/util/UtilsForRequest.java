package com.wts.util;

import com.wts.auth.JwtUtil;
import com.wts.kiwoom.service.KiwoomTokenManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class UtilsForRequest {

    private final JwtUtil jwtUtil;
    private final KiwoomTokenManager kiwoomTokenManager;
    public String attractJwtFromRequest(HttpServletRequest request) {
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
            log.warn(msg);
        }

        return jwt;
    }

    public String getKiwoomTokenFromJwt(String jwt) {

        long userId = Long.parseLong(jwtUtil.getSubject(jwt));
        String tokenId = jwtUtil.getKiwoomTokenRef(jwt);

        Optional<String> kiwoomToken = kiwoomTokenManager.getKiwoomToken(userId, tokenId);

        if (kiwoomToken.isEmpty()) {
            String msg = String.format("키움 토큰을 찾을 수 없음: userId=%d", userId);
            log.warn(msg);
            throw new UsernameNotFoundException(msg);
        }

        return kiwoomToken.get();
    }
}
