package com.mariaribeiro.webhookprocessor.webhook.port.out;

import com.mariaribeiro.webhookprocessor.webhook.domain.model.Source;
import com.mariaribeiro.webhookprocessor.webhook.domain.model.WebhookEvent;

import java.util.Optional;

public interface WebhookEventRepository {
    void save(WebhookEvent event);
    Optional<WebhookEvent> findBySourceAndEventKey(Source source, String eventKey);
}