package com.connto.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "connto.jwt")
public record JwtProperties(String secret, @DefaultValue("86400000") long expirationMs) {}
