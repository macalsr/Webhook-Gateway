package com.mariaribeiro.webhookprocessor.webhook.port.out;

import com.mariaribeiro.webhookprocessor.webhook.domain.model.WebhookEvent;

import java.util.Optional;
import java.util.UUID;

public interface WebhookEventRepository {

    WebhookEvent save(WebhookEvent webhookEvent);

    Optional<WebhookEvent> findBySourceAndEventKey(String source, String eventId);

    Optional<WebhookEvent> findById(UUID id);
}
