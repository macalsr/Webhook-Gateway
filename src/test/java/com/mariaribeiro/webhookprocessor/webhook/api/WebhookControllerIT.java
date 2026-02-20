package com.mariaribeiro.webhookprocessor.webhook.api;

import com.mariaribeiro.webhookprocessor.config.WebhookProperties;
import com.mariaribeiro.webhookprocessor.webhook.application.service.WebhookIngestionService;
import com.mariaribeiro.webhookprocessor.webhook.domain.model.EventStatus;
import com.mariaribeiro.webhookprocessor.webhook.domain.model.WebhookEvent;
import com.mariaribeiro.webhookprocessor.webhook.infrastructure.crypto.HmacSha256Verifier;
import com.mariaribeiro.webhookprocessor.webhook.port.out.WebhookEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class WebhookControllerIT {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WebhookIngestionService ingestionService;

    @MockitoBean
    WebhookEventRepository repository;

    @MockitoBean Clock clock;

    @TestConfiguration
    static class TestConfig {

        @Bean
        WebhookProperties webhookProperties() {
            WebhookProperties p = new WebhookProperties();
            p.setSecrets(Map.of("stripe", "secret-123"));
            return p;
        }

    }

    @Test
    void shouldReturn201WhenSignatureIsValid() throws Exception {
        when(clock.instant()).thenReturn(Instant.parse("2026-01-01T00:10:00Z"));
        String rawBody = """
                {"eventKey":"evt_123","payload":{"hello":"world"}}
                """.trim();

        String timestamp = "1767225900";
        String sigHex = HmacSha256Verifier.signHex("secret-123", timestamp + "." + rawBody);

        when(repository.findBySourceAndEventKey("stripe", "evt_123"))
                .thenReturn(Optional.empty());

        WebhookEvent saved = new WebhookEvent(
                UUID.randomUUID(),
                "stripe",
                "evt_123",
                "{\"hello\":\"world\"}",
                EventStatus.RECEIVED,
                Instant.parse("2026-01-01T00:05:00Z"),
                null
        );

        when(ingestionService.ingest(eq("stripe"), eq("evt_123"), anyString()))
                .thenReturn(saved);

        mockMvc.perform(post("/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Timestamp", timestamp)
                        .header("X-Signature", "sha256=" + sigHex)
                        .content(rawBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/webhooks/stripe/evt_123"))
                .andExpect(jsonPath("$.source").value("stripe"))
                .andExpect(jsonPath("$.eventKey").value("evt_123"));
    }

    @Test
    void shouldReturn200WhenEventIsDuplicate() throws Exception {
        when(clock.instant()).thenReturn(Instant.parse("2026-01-01T00:10:00Z"));
        String rawBody = """
                {"eventKey":"evt_123","payload":{"hello":"world"}}
                """.trim();

        String timestamp = "1767225900";
        String sigHex = HmacSha256Verifier.signHex("secret-123", timestamp + "." + rawBody);

        when(repository.findBySourceAndEventKey("stripe", "evt_123"))
                .thenReturn(Optional.of(mock(WebhookEvent.class))); // já existe

        WebhookEvent existing = new WebhookEvent(
                UUID.randomUUID(),
                "stripe",
                "evt_123",
                "{\"hello\":\"world\"}",
                EventStatus.RECEIVED,
                Instant.parse("2026-01-01T00:05:00Z"),
                null
        );

        when(ingestionService.ingest(eq("stripe"), eq("evt_123"), anyString()))
                .thenReturn(existing);

        mockMvc.perform(post("/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Timestamp", timestamp)
                        .header("X-Signature", "sha256=" + sigHex)
                        .content(rawBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("stripe"))
                .andExpect(jsonPath("$.eventKey").value("evt_123"));
    }

    @Test
    void shouldReturn401WhenSignatureIsInvalid() throws Exception {
        when(clock.instant()).thenReturn(Instant.parse("2026-01-01T00:10:00Z"));
        String rawBody = """
                {"eventKey":"evt_123","payload":{"hello":"world"}}
                """.trim();

        String timestamp = "1767225900";

        mockMvc.perform(post("/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Timestamp", timestamp)
                        .header("X-Signature", "sha256=deadbeef")
                        .content(rawBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenTimestampOutsideReplayWindow() throws Exception {
        when(clock.instant()).thenReturn(Instant.parse("2026-01-01T00:10:00Z"));
        String rawBody = """
                {"eventKey":"evt_123","payload":{"hello":"world"}}
                """.trim();

        // 10 min atrás
        String oldTimestamp = "1767225300";
        String sigHex = HmacSha256Verifier.signHex("secret-123", oldTimestamp + "." + rawBody);

        mockMvc.perform(post("/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Timestamp", oldTimestamp)
                        .header("X-Signature", "sha256=" + sigHex)
                        .content(rawBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn404WhenSourceIsUnknown() throws Exception {
        when(clock.instant()).thenReturn(Instant.parse("2026-01-01T00:10:00Z"));
        String rawBody = """
                {"eventKey":"evt_123","payload":{"hello":"world"}}
                """.trim();

        String timestamp = "1767225900";
        String sigHex = HmacSha256Verifier.signHex("secret-123", timestamp + "." + rawBody);

        mockMvc.perform(post("/webhooks/unknown")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Timestamp", timestamp)
                        .header("X-Signature", "sha256=" + sigHex)
                        .content(rawBody))
                .andExpect(status().isNotFound());
    }
}