package com.supportai.config;

import com.supportai.web.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", c -> c.getPackageName().startsWith("com.supportai.controller"));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Only the public, unauthenticated widget endpoints need throttling.
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/chat/widget/**");
    }
}
