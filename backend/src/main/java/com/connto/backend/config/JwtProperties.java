package com.connto.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "connto.jwt")
public record JwtProperties(String secret, long expirationMs) {}
