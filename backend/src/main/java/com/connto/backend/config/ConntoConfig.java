package com.connto.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    JwtProperties.class,
    OtpProperties.class,
    MediaProperties.class,
    WalletProperties.class
})
public class ConntoConfig {}
