package com.mariaribeiro.webhookprocessor.webhook.infrastructure.persistence;

import com.mariaribeiro.webhookprocessor.webhook.domain.model.EventStatus;
import com.mariaribeiro.webhookprocessor.webhook.domain.model.WebhookEvent;
import com.mariaribeiro.webhookprocessor.webhook.port.out.WebhookEventRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@SpringBootTest
@ActiveProfiles("test")
class WebhookEventRepositoryIT {

    @Autowired
    WebhookEventRepository repository;

    @Test
    @Transactional
    void shouldSaveAndFindBySourceAndEventKey(){
        Instant fixed = Instant.parse("2026-01-01T00:00:00Z");
        WebhookEvent event = new WebhookEvent(
                UUID.randomUUID(),
                "stripe",
                "evt_123",
                "{\"hello\":\"world\"}",
                EventStatus.RECEIVED,
                fixed,
                null
        );

        WebhookEvent event2 = new WebhookEvent(
                UUID.randomUUID(),
                "stripe",
                "evt_123",
                "{\"hello\":\"world\"}",
                EventStatus.RECEIVED,
                fixed,
                null
        );
        repository.save(event);
        Optional<WebhookEvent> bySourceAndEventId = repository.findBySourceAndEventKey("stripe", "evt_123");

        assertThat(bySourceAndEventId).isPresent();
        assertThat(bySourceAndEventId.get().payload()).contains("hello");
    }
}