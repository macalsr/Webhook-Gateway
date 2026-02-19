package com.mariaribeiro.webhookprocessor.webhook.infrastructure.persistence;

import com.mariaribeiro.webhookprocessor.webhook.domain.model.EventStatus;
import com.mariaribeiro.webhookprocessor.webhook.domain.model.WebhookEvent;
import com.mariaribeiro.webhookprocessor.webhook.port.out.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebhookEventRepositoryAdapter implements WebhookEventRepository {

    private final SpringDataWebhookEventRepository repo;

    @Override
    public WebhookEvent save(WebhookEvent webhookEvent) {
        JpaWebhookEventEntity entity = toEntity(webhookEvent);
        JpaWebhookEventEntity saved = repo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<WebhookEvent> findBySourceAndEventKey(String source, String eventId) {
        return repo.findBySourceAndEventKey(source,eventId).map(this::toDomain);
    }

    @Override
    public Optional<WebhookEvent> findById(UUID id) {
        return repo.findById(id).map(this::toDomain);
    }

    private JpaWebhookEventEntity toEntity(WebhookEvent e) {
        JpaWebhookEventEntity entity = new JpaWebhookEventEntity();
        entity.setId(e.id());
        entity.setSource(e.source());
        entity.setEventKey(e.eventKey());
        entity.setPayload(e.payload());
        entity.setStatus(e.status().name());
        entity.setReceivedAt(e.receivedAt());
        entity.setProcessedAt(e.processedAt());
        return entity;
    }

    private WebhookEvent toDomain(JpaWebhookEventEntity e) {
        return new WebhookEvent(
                e.getId(),
                e.getSource(),
                e.getEventKey(),
                e.getPayload(),
                EventStatus.valueOf(e.getStatus()),
                e.getReceivedAt(),
                e.getProcessedAt()
        );
    }
}
