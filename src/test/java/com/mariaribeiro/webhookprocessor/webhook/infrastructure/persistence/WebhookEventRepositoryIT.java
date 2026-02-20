package com.mariaribeiro.webhookprocessor.webhook.infrastructure.persistence;

import com.mariaribeiro.webhookprocessor.webhook.domain.model.EventStatus;
import com.mariaribeiro.webhookprocessor.webhook.domain.model.WebhookEvent;
import com.mariaribeiro.webhookprocessor.webhook.port.out.WebhookEventRepository;
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
    void shouldSaveAndFindBySourceAndEventKey() {
        String eventKey = "evt_" + UUID.randomUUID();

        WebhookEvent event = new WebhookEvent(
                UUID.randomUUID(),
                "stripe",
                eventKey,
                "{\"hello\":\"world\"}",
                EventStatus.RECEIVED,
                Instant.now(),
                null
        );

        repository.save(event);

        Optional<WebhookEvent> found =
                repository.findBySourceAndEventKey("stripe", eventKey);

        assertThat(found).isPresent();
        assertThat(found.get().payload()).contains("hello");
    }
}