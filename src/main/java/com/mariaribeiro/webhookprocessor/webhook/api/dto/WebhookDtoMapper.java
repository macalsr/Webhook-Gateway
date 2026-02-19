package com.mariaribeiro.webhookprocessor.webhook.api.dto;

import com.mariaribeiro.webhookprocessor.webhook.domain.model.WebhookEvent;

public final class WebhookDtoMapper {


    public static WebhookIngestionResponse toResponse(WebhookEvent webhookEvent) {
        return new WebhookIngestionResponse(
                webhookEvent.id(),
                webhookEvent.source(),
                webhookEvent.eventKey(),
                webhookEvent.status(),
                webhookEvent.receivedAt(),
                webhookEvent.processedAt()
        );
    }
}
