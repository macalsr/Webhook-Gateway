package com.mariaribeiro.webhookprocessor.webhook.domain.model;

import java.time.Instant;
import java.util.UUID;

public record WebhookEvent(
        UUID id,
        String source,
        String eventKey,
        String payload,
        EventStatus status,
        Instant receivedAt,
        Instant processedAt) {
}
