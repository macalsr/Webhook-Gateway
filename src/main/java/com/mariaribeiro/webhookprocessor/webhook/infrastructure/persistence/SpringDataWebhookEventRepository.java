package com.mariaribeiro.webhookprocessor.webhook.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataWebhookEventRepository extends JpaRepository<WebhookEventEntity, UUID> {

    Optional<WebhookEventEntity> findBySourceAndEventKey(String source, String eventKey);
}
