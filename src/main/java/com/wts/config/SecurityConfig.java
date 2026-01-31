// 보안 설정: WebFlux 환경에 맞게 ServerHttpSecurity/SecurityWebFilterChain으로 전환하였으며, /auth/** 포함하여 공개 경로를 허용합니다.
// 주요 책임: Spring Security 필터 체인 구성
package com.wts.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.wts.auth.JwtUtil;
import com.wts.auth.security.CustomOAuth2UserService;
import com.wts.auth.security.OAuth2AuthenticationSuccessHandler;
import com.wts.auth.security.JwtAuthenticationFilter;
import com.wts.api.repository.UserRepository;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final OAuth2AuthenticationSuccessHandler successHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public SecurityConfig(OAuth2AuthenticationSuccessHandler successHandler,
                          CustomOAuth2UserService customOAuth2UserService,
                          JwtUtil jwtUtil,
                          UserRepository userRepository) {
        this.successHandler = successHandler;
        this.customOAuth2UserService = customOAuth2UserService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil, userRepository);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:19789"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스 (프론트엔드 빌드 결과물) 공개
                        .requestMatchers("/", "/login", "/index.html", "/assets/**", "/favicon.ico",
                                       "/*.js", "/*.css", "/*.png", "/*.jpg", "/*.svg", "/*.ico").permitAll()
                        // 키움 인증 엔드포인트 공개 (JWT 발급용)
                        .requestMatchers("/api/kiwoom/authenticate", "/api/kiwoom/public/**", "/api/guest/**").permitAll()
                        // OAuth2 및 인증 관련 경로 공개
                        .requestMatchers("/ws/**", "/actuator/**", "/auth/**", "/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/api/test/**").permitAll()
                        // 키움 API는 JWT 인증 필요
                        .requestMatchers("/api/kiwoom/**").authenticated()
                        // 기타 API는 인증 필요
                        .requestMatchers("/api/**").authenticated()
                        // 나머지 요청은 공개 (SPA 라우팅 지원)
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(successHandler)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            String requestUri = request.getRequestURI();

                            // API 요청은 401 반환
                            if (requestUri.startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                                return;
                            }

                            // 정적 리소스는 index.html로 포워드 (SPA 라우팅 지원)
                            if (!requestUri.startsWith("/oauth2/") && !requestUri.startsWith("/login/oauth2/")) {
                                request.getRequestDispatcher("/index.html").forward(request, response);
                                return;
                            }

                            // OAuth2 콜백 요청은 기본 처리
                            response.sendRedirect("/oauth2/authorization/google");
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
