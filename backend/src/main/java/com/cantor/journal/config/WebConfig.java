package com.cantor.journal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * 빌드된 프론트엔드(React/Vite)를 classpath:/static 에서 서빙한다.
 * 존재하지 않는 경로(SPA 클라이언트 라우트)는 index.html 로 폴백하고,
 * /api 요청은 정적 리소스로 처리하지 않는다(REST 컨트롤러가 담당).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(@NonNull String resourcePath, @NonNull Resource location)
                            throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // API 경로는 정적 폴백 대상이 아니다.
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }
                        // SPA 라우트: index.html 로 폴백
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
