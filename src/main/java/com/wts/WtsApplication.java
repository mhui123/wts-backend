// 애플리케이션 진입점: Spring Boot 애플리케이션을 시작합니다.
// 주요 책임: 애플리케이션 컨텍스트 초기화 및 내장 서버 실행
package com.wts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WtsApplication {
    public static void main(String[] args) {
        SpringApplication.run(WtsApplication.class, args);
    }
}
