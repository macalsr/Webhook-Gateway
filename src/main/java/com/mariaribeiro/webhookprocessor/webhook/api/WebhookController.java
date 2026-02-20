package com.mariaribeiro.webhookprocessor.webhook.api;


import com.mariaribeiro.webhookprocessor.config.WebhookProperties;
import com.mariaribeiro.webhookprocessor.webhook.api.dto.WebhookDtoMapper;
import com.mariaribeiro.webhookprocessor.webhook.api.dto.WebhookIngestionResponse;
import com.mariaribeiro.webhookprocessor.webhook.application.service.WebhookIngestionService;
import com.mariaribeiro.webhookprocessor.webhook.domain.model.WebhookEvent;
import com.mariaribeiro.webhookprocessor.webhook.infrastructure.crypto.HmacSha256Verifier;
import com.mariaribeiro.webhookprocessor.webhook.port.out.WebhookEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Clock;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/webhooks", produces = APPLICATION_JSON_VALUE)
public class WebhookController {

    private static final long REPLAY_WINDOWS_SECONDS = 300;

    private final WebhookIngestionService ingestionService;
    private final WebhookEventRepository repository;
    private final ObjectMapper objectMapper;
    private final WebhookProperties webhookProperties;
    private final Clock clock;

    public WebhookController(WebhookIngestionService ingestionService, WebhookEventRepository repository, ObjectMapper objectMapper, WebhookProperties webhookProperties, Clock clock) {
        this.ingestionService = ingestionService;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.webhookProperties = webhookProperties;
        this.clock = clock;
    }

    @PostMapping(value = "/{source}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<WebhookIngestionResponse> ingest(@PathVariable String source,
                                                           @RequestHeader(name = "X-Signature", required = false) String signature,
                                                           @RequestHeader(name = "X-Timestamp", required = false) String timestamp,
                                                           @RequestBody String rawBody) {
        if (signature == null || signature.isBlank()) return ResponseEntity.status(401).build();
        if (timestamp == null || timestamp.isBlank()) return ResponseEntity.status(401).build();
        long ts;

        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(401).build();
        }

        long now = clock.instant().getEpochSecond();
        if (Math.abs(now - ts) > REPLAY_WINDOWS_SECONDS) {
            return ResponseEntity.status(401).build();
        }

        final String secret;
        try {
            secret = webhookProperties.secretFor(source);
        } catch (Exception e) {
            return ResponseEntity.status(404).build();
        }

        String expected = !timestamp.isBlank()
                ? HmacSha256Verifier.signHex(secret, timestamp + "." + rawBody)
                : HmacSha256Verifier.signHex(secret, rawBody);

        String provided = signature.startsWith("sha256=") ? signature.substring(7) : signature;

        if (!HmacSha256Verifier.constantTimeEquals(expected, provided)) {
            return ResponseEntity.status(401).build();
        }

        JsonNode node = objectMapper.readTree(rawBody);
        String eventKey = node.hasNonNull("eventKey") ? node.get("eventKey").asText() : null;
        JsonNode payload = node.get("payload");

        if (eventKey == null || eventKey.isBlank() || payload == null) {
            return ResponseEntity.badRequest().build();
        }

        boolean alreadyExist = repository.findBySourceAndEventKey(source, eventKey).isPresent();

        String payloadAsString = objectMapper.writeValueAsString(payload);

        WebhookEvent event = ingestionService.ingest(source, eventKey, payloadAsString);

        WebhookIngestionResponse body = WebhookDtoMapper.toResponse(event);

        if (alreadyExist) {
            return ResponseEntity.ok(body);
        }

        URI location = URI.create("/webhooks/" + source + "/" + eventKey);
        return ResponseEntity.created(location).body(body);
    }
}
