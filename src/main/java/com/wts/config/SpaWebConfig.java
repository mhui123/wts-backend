package com.wts.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpaWebConfig implements WebMvcConfigurer {
    String[] SPA_PATHS = { "/", "/login", "/kiwoom-api", "/upload", "/trade-history" };

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        for (String path : SPA_PATHS) {
            registry.addViewController(path).setViewName("forward:/index.html");
            registry.addViewController(path + "/**").setViewName("forward:/index.html");
        }
    }
}
