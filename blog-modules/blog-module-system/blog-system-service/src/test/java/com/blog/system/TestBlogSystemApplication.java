package com.blog.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication(scanBasePackages = "com.blog.system.controller", exclude = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class })
@org.springframework.context.annotation.Import(com.blog.system.config.TestSecurityConfig.class)
@EnableMethodSecurity
public class TestBlogSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestBlogSystemApplication.class, args);
    }
}
