package com.mariaribeiro.webhookprocessor.webhook.domain.model;

import lombok.Getter;

@Getter
public enum Source {
    STRIPE, SHOPIFY, GENERIC;

    public static Source from(String raw) {
        if (raw == null || raw.isBlank()) return GENERIC;
        return switch (raw.trim().toLowerCase()) {
            case "stripe" -> STRIPE;
            case "shopify" -> SHOPIFY;
            default -> GENERIC;
        };
    }
}
