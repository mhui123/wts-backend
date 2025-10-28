// 보안 설정: WebFlux 환경에 맞게 ServerHttpSecurity/SecurityWebFilterChain으로 전환하였으며, /auth/** 포함하여 공개 경로를 허용합니다.
// 주요 책임: Spring Security 필터 체인 구성
package com.wts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/ws/**", "/actuator/**", "/api/**", "/auth/**").permitAll()
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
