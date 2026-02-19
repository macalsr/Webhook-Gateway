package com.mariaribeiro.webhookprocessor.webhook.api;


import com.mariaribeiro.webhookprocessor.webhook.api.dto.WebhookDtoMapper;
import com.mariaribeiro.webhookprocessor.webhook.api.dto.WebhookIngestionRequest;
import com.mariaribeiro.webhookprocessor.webhook.api.dto.WebhookIngestionResponse;
import com.mariaribeiro.webhookprocessor.webhook.application.service.WebhookIngestionService;
import com.mariaribeiro.webhookprocessor.webhook.domain.model.WebhookEvent;
import com.mariaribeiro.webhookprocessor.webhook.port.out.WebhookEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/webhooks", produces = APPLICATION_JSON_VALUE)
public class WebhookController {

    private final WebhookIngestionService ingestionService;
    private final WebhookEventRepository repository;
    private final ObjectMapper objectMapper;


    public WebhookController(WebhookIngestionService ingestionService, WebhookEventRepository repository, ObjectMapper objectMapper) {
        this.ingestionService = ingestionService;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/{source}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<WebhookIngestionResponse> ingest(@PathVariable String source, @RequestBody WebhookIngestionRequest request) {

        if (request == null || request.eventKey() == null || request.eventKey().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (request.payload() == null) {
            return ResponseEntity.badRequest().build();
        }

        boolean alreadyExist = repository.findBySourceAndEventKey(source, request.eventKey()).isPresent();

        String payloadAsString = objectMapper.writeValueAsString(request.payload());

        WebhookEvent event = ingestionService.ingest(source, request.eventKey(), payloadAsString);

        WebhookIngestionResponse body = WebhookDtoMapper.toResponse(event);

        if(alreadyExist){
            return ResponseEntity.ok(body);
        }

        URI location = URI.create("/webhooks/" + source + "/" + request.eventKey());
        return ResponseEntity.created(location).body(body);
    }
}
