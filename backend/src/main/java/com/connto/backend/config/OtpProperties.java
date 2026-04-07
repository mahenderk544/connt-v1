package com.connto.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "connto.otp")
public record OtpProperties(int ttlSeconds, int maxAttempts, boolean devReturnCode) {}
