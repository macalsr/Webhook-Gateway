package com.mariaribeiro.webhookprocessor.webhook.api.dto;

import com.mariaribeiro.webhookprocessor.webhook.domain.model.EventStatus;

import java.time.Instant;
import java.util.UUID;

public record WebhookIngestionResponse(
        UUID id,
        String source,
        String eventKey,
        EventStatus status,
        Instant createdAt,
        Instant processedAt
) {
}
