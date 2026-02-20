package com.mariaribeiro.webhookprocessor.webhook.infrastructure.persistence;

import com.mariaribeiro.webhookprocessor.webhook.domain.model.WebhookEvent;

final class WebhookEventMapper {

    static WebhookEventEntity toEntity(WebhookEvent e) {
        return new WebhookEventEntity(
                e.id(),
                e.source(),
                e.eventKey(),
                e.payload(),
                e.status(),
                e.receivedAt(),
                e.processedAt()
        );
    }

    static WebhookEvent toDomain(WebhookEventEntity e) {
        return new WebhookEvent(
                e.getId(),
                e.getSource(),
                e.getEventKey(),
                e.getPayload(),
                e.getStatus(),
                e.getReceivedAt(),
                e.getProcessedAt()
        );
    }

    private WebhookEventMapper() { }
}

