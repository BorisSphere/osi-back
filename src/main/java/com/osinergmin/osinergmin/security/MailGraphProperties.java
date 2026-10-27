package com.osinergmin.osinergmin.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailGraphProperties(
        boolean enabled,
        String from,
        String frontUrl,
        Graph graph) {

    public record Graph(String tenantId, String clientId, String clientSecret) {
    }
}
