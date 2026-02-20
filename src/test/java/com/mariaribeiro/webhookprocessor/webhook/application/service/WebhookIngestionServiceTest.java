package com.mariaribeiro.webhookprocessor.webhook.application.service;

import com.mariaribeiro.webhookprocessor.webhook.domain.model.EventStatus;
import com.mariaribeiro.webhookprocessor.webhook.domain.model.WebhookEvent;
import com.mariaribeiro.webhookprocessor.webhook.port.out.WebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookIngestionServiceTest {

    @Mock
    WebhookEventRepository repository;

    Clock fixedClock;

    @InjectMocks
    WebhookIngestionService service;

    @BeforeEach
    void setUp() throws Exception {
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), Clock.systemUTC().getZone());
        Field clockField = WebhookIngestionService.class.getDeclaredField("clock");
        clockField.setAccessible(true);
        clockField.set(service, fixedClock);
    }

    @Test
    void shouldCreateAndSaveWhenEventDoesNotExist() {
        when(repository.findBySourceAndEventKey("stripe", "evt_123"))
                .thenReturn(Optional.empty());

        ArgumentCaptor<WebhookEvent> captor = ArgumentCaptor.forClass(WebhookEvent.class);

        WebhookEvent saved = new WebhookEvent(
                UUID.randomUUID(),
                "stripe",
                "evt_123",
                "{\"hello\":\"world\"}",
                EventStatus.RECEIVED,
                Instant.parse("2026-01-01T00:00:00Z"),
                null
        );

        when(repository.save(any(WebhookEvent.class))).thenReturn(saved);

        WebhookEvent result = service.ingest("stripe", "evt_123", "{\"hello\":\"world\"}");

        verify(repository).save(captor.capture());
        WebhookEvent toSave = captor.getValue();

        assertThat(toSave.source()).isEqualTo("stripe");
        assertThat(toSave.eventKey()).isEqualTo("evt_123");
        assertThat(toSave.payload()).contains("hello");
        assertThat(toSave.status()).isEqualTo(EventStatus.RECEIVED);
        assertThat(toSave.receivedAt()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
        assertThat(toSave.processedAt()).isNull();

        assertThat(result).isEqualTo(saved);
    }

    @Test
    void shouldReturnExistingAndNotSaveWhenDuplicate() {
        WebhookEvent existing = new WebhookEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "stripe",
                "evt_123",
                "{\"hello\":\"world\"}",
                EventStatus.RECEIVED,
                Instant.parse("2025-12-31T23:00:00Z"),
                null
        );

        when(repository.findBySourceAndEventKey("stripe", "evt_123"))
                .thenReturn(Optional.of(existing));

        WebhookEvent result = service.ingest("stripe", "evt_123", "{\"hello\":\"world\"}");

        verify(repository, never()).save(any());
        assertThat(result).isEqualTo(existing);
    }
}