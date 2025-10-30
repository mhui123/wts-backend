package com.wts.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wts.auth.JwtUtil;
import com.wts.entity.User;
import com.wts.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.frontend.success-url:http://localhost:5173}")
    private String frontendSuccessUrl;

    public OAuth2AuthenticationSuccessHandler(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        try {
            // registrationId: try to call getAuthorizedClientRegistrationId() on the authentication if present
            String registrationId = null;
            try {
                Method regMethod = authentication.getClass().getMethod("getAuthorizedClientRegistrationId");
                Object regObj = regMethod.invoke(authentication);
                if (regObj != null) registrationId = String.valueOf(regObj);
            } catch (NoSuchMethodException nsme) {
                // ignore, fall back
            }
            if (registrationId == null) registrationId = "unknown";

            Object principal = authentication.getPrincipal();
            Map<String, Object> attrs = Collections.emptyMap();

            if (principal instanceof Map) {
                attrs = (Map<String, Object>) principal;
            } else {
                try {
                    Method getAttrs = principal.getClass().getMethod("getAttributes");
                    Object obj = getAttrs.invoke(principal);
                    if (obj instanceof Map) {
                        attrs = (Map<String, Object>) obj;
                    }
                } catch (NoSuchMethodException nsme) {
                    // no getAttributes - leave attrs empty
                }
            }

            // Provider user id
            String providerId = attrs.containsKey("sub") ? String.valueOf(attrs.get("sub")) : String.valueOf(attrs.getOrDefault("id", ""));
            String email = attrs.containsKey("email") ? String.valueOf(attrs.get("email")) : null;
            String name = attrs.containsKey("name") ? String.valueOf(attrs.get("name")) : (String) attrs.getOrDefault("login", null);
            String picture = attrs.containsKey("picture") ? String.valueOf(attrs.get("picture")) : null;

            Optional<User> found = userRepository.findByProviderAndProviderId(registrationId, providerId);
            User user;
            if (found.isPresent()) {
                user = found.get();
                user.setName(name != null ? name : user.getName());
                user.setPictureUrl(picture != null ? picture : user.getPictureUrl());
                user.setLastLoginAt(LocalDateTime.now());
            } else {
                // try by email to merge
                if (email != null) {
                    Optional<User> byEmail = userRepository.findByEmail(email);
                    if (byEmail.isPresent()) {
                        user = byEmail.get();
                        user.setProvider(registrationId);
                        user.setProviderId(providerId);
                        user.setName(name != null ? name : user.getName());
                        user.setPictureUrl(picture != null ? picture : user.getPictureUrl());
                        user.setLastLoginAt(LocalDateTime.now());
                    } else {
                        user = new User(registrationId, providerId, email, name, picture);
                    }
                } else {
                    user = new User(registrationId, providerId, null, name, picture);
                }
            }

            user.setLastLoginAt(LocalDateTime.now());
            User saved = userRepository.save(user);

            String jwt = jwtUtil.createToken(String.valueOf(saved.getId()));

            // JWT를 HttpOnly + Secure + SameSite=None 쿠키로 설정 (크로스-사이트 요청에 포함되도록)
            int maxAge = 60 * 60 * 24; // 1 day
            String cookieValue = URLEncoder.encode(jwt, StandardCharsets.UTF_8);
            String setCookie = "JWT=" + cookieValue
                    + "; Path=/"
                    + "; Max-Age=" + maxAge
                    + "; HttpOnly"
                    + "; Secure"
                    + "; SameSite=None";
            response.addHeader("Set-Cookie", setCookie);

            // 프론트로 리다이렉트 (쿠키 기반 인증 사용)
            response.sendRedirect(frontendSuccessUrl);
        } catch (Exception e) {
            log.error("OAuth2 success handler failed", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
