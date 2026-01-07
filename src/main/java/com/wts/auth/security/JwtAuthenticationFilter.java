package com.wts.auth.security;

import com.wts.auth.JwtUtil;
import com.wts.api.entity.User;
import com.wts.api.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        log.debug("JWT Filter processing request: {}", requestUri);

        try {
            String token = resolveToken(request);
            if (StringUtils.hasText(token)) {
                log.debug("JWT token found for URI: {}", requestUri);
                if (jwtUtil.validateToken(token)) {
                    log.debug("JWT token is valid for URI: {}", requestUri);
                    String subject = jwtUtil.getSubject(token); // user id or email
                    if (subject != null) {
                        log.debug("JWT subject: {} for URI: {}", subject, requestUri);
                        Optional<User> userOpt = findUserBySubject(subject);
                        if (userOpt.isPresent()) {
                            User user = userOpt.get();
                            log.debug("User found: {} (provider: {}) for URI: {}", user.getEmail(), user.getProvider(), requestUri);

                            // 사용자 권한 설정
                            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                            if (user.getRoles() != null) {
                                String[] roles = user.getRoles().split(",");
                                for (String role : roles) {
                                    authorities.add(new SimpleGrantedAuthority(role.trim()));
                                }
                            }
                            log.debug("User authorities: {} for URI: {}", authorities, requestUri);

                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(user, null, authorities);
                            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                            log.debug("Authentication set successfully for user: {} URI: {}", user.getEmail(), requestUri);
                        } else {
                            log.warn("User not found for subject: {} URI: {}", subject, requestUri);
                        }
                    } else {
                        log.warn("JWT subject is null for URI: {}", requestUri);
                    }
                } else {
                    log.warn("JWT token validation failed for URI: {}", requestUri);
                }
            } else {
                log.debug("No JWT token found for URI: {}", requestUri);
            }
        } catch (Exception e) {
            log.error("JWT filter error for URI: {} - {}", requestUri, e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    /**
     * subject가 숫자인 경우 ID로, 그렇지 않으면 email로 사용자를 찾습니다.
     */
    private Optional<User> findUserBySubject(String subject) {
        try {
            // subject가 숫자인지 확인 (기존 사용자 ID)
            Long userId = Long.valueOf(subject);
            return userRepository.findById(userId);
        } catch (NumberFormatException e) {
            // subject가 숫자가 아닌 경우 email로 검색 (게스트 사용자)
            return userRepository.findByEmail(subject);
        }
    }

    private String resolveToken(HttpServletRequest request) {
        // Authorization: Bearer <token>
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        // Cookie: JWT=<token>
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("JWT".equals(c.getName()) && StringUtils.hasText(c.getValue())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }
}
