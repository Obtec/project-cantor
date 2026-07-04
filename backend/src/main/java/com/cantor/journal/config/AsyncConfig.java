package com.cantor.journal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 실행 활성화 (AI 리뷰 생성 등 오래 걸리는 작업용).
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
