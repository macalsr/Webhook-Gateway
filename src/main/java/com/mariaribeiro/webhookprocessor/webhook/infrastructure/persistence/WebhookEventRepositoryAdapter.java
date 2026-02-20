package com.mariaribeiro.webhookprocessor.webhook.infrastructure.persistence;

import com.mariaribeiro.webhookprocessor.webhook.domain.model.WebhookEvent;
import com.mariaribeiro.webhookprocessor.webhook.port.out.WebhookEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

import static com.mariaribeiro.webhookprocessor.webhook.infrastructure.persistence.WebhookEventMapper.toDomain;
import static com.mariaribeiro.webhookprocessor.webhook.infrastructure.persistence.WebhookEventMapper.toEntity;

@Component
@RequiredArgsConstructor
public class WebhookEventRepositoryAdapter implements WebhookEventRepository {

    private final SpringDataWebhookEventRepository repo;

    @Override
    @Transactional
    public WebhookEvent save(WebhookEvent webhookEvent) {
        WebhookEventEntity entity = toEntity(webhookEvent);
        WebhookEventEntity saved = repo.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional
    public Optional<WebhookEvent> findBySourceAndEventKey(String source, String eventId) {
        return repo.findBySourceAndEventKey(source,eventId).map(WebhookEventMapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<WebhookEvent> findById(UUID id) {
        return repo.findById(id).map(WebhookEventMapper::toDomain);
    }

}
