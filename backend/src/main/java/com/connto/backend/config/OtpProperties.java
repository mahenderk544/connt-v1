package com.connto.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "connto.otp")
public record OtpProperties(
        @DefaultValue("300") int ttlSeconds,
        @DefaultValue("5") int maxAttempts,
        boolean devReturnCode) {}
