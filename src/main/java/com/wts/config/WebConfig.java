package com.wts.config;

import com.wts.kiwoom.interceptor.KiwoomPermissionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private KiwoomPermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor)
                // 공개 API를 제외한 구체적인 패턴만 적용
                .addPathPatterns("/api/kiwoom/account/**")
                .addPathPatterns("/api/kiwoom/trade/**")
                .addPathPatterns("/api/kiwoom/balance/**")
                .addPathPatterns("/api/kiwoom/order/**")
                // public 경로는 아예 패턴에 포함하지 않음
                .excludePathPatterns("/api/kiwoom/public/**"); // 이중 보장
    }
}