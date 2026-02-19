package com.mariaribeiro.webhookprocessor.webhook.application.service;

import com.mariaribeiro.webhookprocessor.webhook.domain.model.EventStatus;
import com.mariaribeiro.webhookprocessor.webhook.domain.model.WebhookEvent;
import com.mariaribeiro.webhookprocessor.webhook.port.out.WebhookEventRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.apache.logging.log4j.util.Strings.isBlank;


@Service
public class WebhookIngestionService {

    private final WebhookEventRepository repository;
    private final Clock clock;


    public WebhookIngestionService(WebhookEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public WebhookEvent ingest(String source, String eventKey, String payload){
        validate(source,eventKey,payload);

        Optional<WebhookEvent> existing = repository.findBySourceAndEventKey(source,eventKey);

        if(existing.isPresent()){
            return existing.get();
        }

        WebhookEvent toSave = new WebhookEvent(
                UUID.randomUUID(),
                source,
                eventKey,
                payload,
                EventStatus.RECEIVED,
                Instant.now(clock),
                null
        );

        return repository.save(toSave);
    }

    private static void validate(String source, String eventKey, String payload){
        if(isBlank(source)){
            throw new IllegalArgumentException("Source cannot be blank");
        }
        if(isBlank(eventKey)){
            throw new IllegalArgumentException("EventKey cannot be blank");
        }
        if(Objects.isNull(payload)){
            throw new IllegalArgumentException("Payload cannot be blank");
        }
    }
}
