package com.workflow.approval.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 应用启动后执行内置数据初始化（admin 账号、内置部门与用户）。
 * 具体逻辑见 {@link DataSeeder}。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    @Bean
    public ApplicationRunner initDataRunner(DataSeeder dataSeeder) {
        return args -> dataSeeder.seed();
    }
}
