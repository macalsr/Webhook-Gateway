package com.mariaribeiro.webhookprocessor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@ConfigurationProperties(prefix = "webhook")
public class WebhookProperties {

    private Map<String, String> secrets = new HashMap<>();

    public String secretFor(String source) {
        String secret = secrets.get(source);
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Unknown source or missing secret: " + source);
        }
        return secret;
    }

}
