package com.yoneodoo.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "yoneodoo.admin")
public record AdminProperties(String secret) {

    public boolean hasSecret() {
        return secret != null && !secret.isBlank();
    }
}
