package com.mariaribeiro.webhookprocessor.webhook.domain.model;
import java.time.Instant;
import java.util.UUID;

public record WebhookEvent(
        UUID id,
        Source source,
        String eventKey,
        String payload,
        Instant receivedAt,
        EventStatus status
) {
    public static WebhookEvent newEvent(UUID id, Source source, String eventKey, String payload, Instant receivedAt) {
        return new WebhookEvent(id, source, eventKey, payload, receivedAt, EventStatus.RECEIVED);
    }
}